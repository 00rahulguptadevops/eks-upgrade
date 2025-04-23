def sendMessage(String status, String color, String stageName, String slackChannel, String errorMessage = "") {
    def message = ""
    switch (status) {
        case "start":
            message = ":rocket: Stage *${stageName}* started in job *${env.JOB_NAME}* (Build #${env.BUILD_NUMBER})"
            break
        case "success":
            message = ":white_check_mark: Stage *${stageName}* completed successfully"
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
        body()
        sendMessage("success", "good", stageName, slackChannel)
    } catch (err) {
        sendMessage("failure", "danger", stageName, slackChannel, err.getMessage())
        throw err
    }
}

