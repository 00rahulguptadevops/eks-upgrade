def sendMessage(String status, String color, String stageName, String slackChannel, String errorMessage = "", String summary = "") {
    def message = ""
    switch (status) {
        case "start":
            message = ":rocket: Stage *${stageName}* started in job *${env.JOB_NAME}* (Build #${env.BUILD_NUMBER})"
            break
        case "success":
            message = ":white_check_mark: Stage *${stageName}* completed successfully"
            if (summary) {
                message += "\n\n*Details:*\n${summary}"
            }
            break
        case "failure":
            message = ":x: Stage *${stageName}* failed\n*Reason:* `${errorMessage}`"
            break
    }

    slackSend channel: slackChannel,
              color: color,
              message: message
}

def notifyStage(String stageName, String slackChannel, Closure body) {
    sendMessage("start", "#439FE0", stageName, slackChannel)
    try {
        def result = body()
        def summary = result?.summary ?: ""
        def outputFilePath = result?.file ?: ""

        sendMessage("success", "good", stageName, slackChannel, "", summary)

        if (outputFilePath) {
            slackUploadFile(
                channel: slackChannel,
                filePath: outputFilePath,
                filename: outputFilePath.split("/")[-1],
                initialComment: ":page_facing_up: *Full output for stage `${stageName}`*"
            )
        }
    } catch (err) {
        sendMessage("failure", "danger", stageName, slackChannel, err.getMessage())
        throw err
    }
}

