def sendMessage(String status, String color, String webhookCredId, String stageName) {
    def message = ""
    switch (status) {
        case "start":
            message = ":rocket: Job *${env.JOB_NAME}* started (Build #${env.BUILD_NUMBER}) in stage *${stageName}*"
            break
        case "success":
            message = ":white_check_mark: Job *${env.JOB_NAME}* completed successfully (Build #${env.BUILD_NUMBER}) at stage *${stageName}*"
            break
        case "failure":
            message = ":x: Job *${env.JOB_NAME}* failed (Build #${env.BUILD_NUMBER}) at stage *${stageName}*"
            break
        default:
            message = ":grey_question: Job status unknown"
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

def notifyStart(String webhookCredId, String stageName = "N/A") {
    sendMessage("start", "#439FE0", webhookCredId, stageName)
}

def notifySuccess(String webhookCredId, String stageName = "N/A") {
    sendMessage("success", "good", webhookCredId, stageName)
}

def notifyFailure(String webhookCredId, String stageName = "N/A") {
    sendMessage("failure", "danger", webhookCredId, stageName)
}

