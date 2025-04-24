def sendMessage(String status, String color, String stageName, String slackChannel, String errorMessage = "", String summary = "", String fileToUpload = "") {
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

    if (status == "failure" && fileToUpload?.trim()) {
        uploadFileToSlack(fileToUpload, slackChannel)
    }
}

def notifyStage(String stageName, String slackChannel, Closure body) {
    sendMessage("start", "#439FE0", stageName, slackChannel)
    try {
        def summary = body()
        sendMessage("success", "good", stageName, slackChannel, "", summary ?: "")
    } catch (err) {
        def outputFile = 'deprecated_output.json'
        sendMessage("failure", "danger", stageName, slackChannel, err.getMessage(), "", outputFile)
        throw err
    }
}

def uploadFileToSlack(String filePath, String slackChannel) {
    withCredentials([string(credentialsId: 'slack-token', variable: 'SLACK_TOKEN')]) {
        sh """
            curl -F file=@${filePath} \\
                 -F "channels=${slackChannel}" \\
                 -F "initial_comment=Deprecated API Report - Build #${env.BUILD_NUMBER}" \\
                 -F "title=${filePath}" \\
                 -H "Authorization: Bearer ${SLACK_TOKEN}" \\
                 https://slack.com/api/files.upload
        """
    }
}

