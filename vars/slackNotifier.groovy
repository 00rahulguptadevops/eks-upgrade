def sendMessage(String status, String color, String webhookCredId, String stageName) {
    def message = ""
    switch (status) {
        case "start":
            message = ":rocket: Stage *${stageName}* started in job *${env.JOB_NAME}* (Build #${env.BUILD_NUMBER})"
            break
        case "success":
            message = ":white_check_mark: Stage *${stageName}* completed successfully"
            break
        case "failure":
            message = ":x: Stage *${stageName}* failed"
            break
    }

    withCredentials([string(credentialsId: webhookCredId, variable: 'SLACK_WEBHOOK')]) {
        def payload = """
        {
            "attachments": [
                {
                    "color": "${color}",
                    "text": "${message}"
                }
            ]
        }
        """
        httpRequest httpMode: 'POST',
                    contentType: 'APPLICATION_JSON',
                    requestBody: payload,
                    url: SLACK_WEBHOOK
    }
}

def notifyStage(String stageName, String webhookCredId, Closure body) {
    sendMessage("start", "#439FE0", webhookCredId, stageName)
    try {
        body()
        sendMessage("success", "good", webhookCredId, stageName)
    } catch (err) {
        sendMessage("failure", "danger", webhookCredId, stageName)
        throw err
    }
}

