def notifyStart(String channel, String tokenCredentialId) {
    slackSend channel: channel, tokenCredentialId: tokenCredentialId, color: '#439FE0',
              message: ":rocket: Job *${env.JOB_NAME}* started (Build #${env.BUILD_NUMBER})"
}

def notifySuccess(String channel, String tokenCredentialId) {
    slackSend channel: channel, tokenCredentialId: tokenCredentialId, color: 'good',
              message: ":white_check_mark: Job *${env.JOB_NAME}* completed successfully (Build #${env.BUILD_NUMBER})"
}

def notifyFailure(String channel, String tokenCredentialId) {
    slackSend channel: channel, tokenCredentialId: tokenCredentialId, color: 'danger',
              message: ":x: Job *${env.JOB_NAME}* failed (Build #${env.BUILD_NUMBER})"
}

