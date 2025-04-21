def sendMessage(String status, String color, String webhookCredId) {
    def message = ""
    switch (status) {
        case "start":
            message = ":rocket: Job *${env.JOB_NAME}* started (Build #${env.BUILD_NUMBER})"
            break
        case "success":
            message = ":white_check_mark: Job *${env.JOB_NAME}* completed successfully (Build #${env.BUILD_NUMBER})"
            break
        case "failure":
            message = ":x: Job *${env.JOB_NAME}* failed (Build #${env.BUILD_NUMBER})"
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

def notifyStart(String webhookCredId) {
    sendMessage("start", "#439FE0", webhookCredId)
}

def notifySuccess(String webhookCredId) {
    sendMessage("success", "good", webhookCredId)
}

def notifyFailure(String webhookCredId) {
    sendMessage("failure", "danger", webhookCredId)
}

