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
        def outputFile = 'output.json'
        sendMessage("failure", "danger", stageName, slackChannel, err.getMessage(), "", outputFile)
        throw err
    }
}

def uploadFileToSlack(String filePath, String slackChannel) {
    withCredentials([string(credentialsId: 'slack-token', variable: 'SLACK_TOKEN')]) {
        // If file path is invalid or file doesn't exist
        if (!filePath || !fileExists(filePath)) {
            error "File does not exist: ${filePath}"
        }

        // Get file size for upload
        def fileSize = sh(script: "stat -c %s ${filePath}", returnStdout: true).trim()

        // Request the upload URL from Slack
        def uploadUrlResponse = sh(script: """
            curl -s -X POST https://slack.com/api/files.getUploadURLExternal \
                -H "Authorization: Bearer \$SLACK_TOKEN" \
                -H "Content-Type: application/json" \
                -d '{
                    "filename": "\$(basename ${filePath})",  // <-- Escaped $ for basename
                    "length": ${fileSize},
                    "channels": ["${slackChannel}"]
                }'
            """, returnStdout: true).trim()

        // Parse the upload URL and file ID from the response
        def uploadUrl = readJSON(text: uploadUrlResponse).upload_url
        def fileId = readJSON(text: uploadUrlResponse).file_id

        if (!uploadUrl) {
            error "Failed to obtain upload URL from Slack: ${uploadUrlResponse}"
        }

        // Upload the file to Slack
        sh """
            curl -s -X POST ${uploadUrl} \
                -H "Authorization: Bearer \$SLACK_TOKEN" \
                -H "Content-Type: application/octet-stream" \
                --data-binary @${filePath}
        """

        // Complete the file upload
        def completeResponse = sh(script: """
            curl -s -X POST https://slack.com/api/files.completeUploadExternal \
                -H "Authorization: Bearer \$SLACK_TOKEN" \
                -H "Content-Type: application/json" \
                -d '{
                    "file_id": "${fileId}",
                    "title": "Build #${env.BUILD_NUMBER} - ${filePath}",
                    "initial_comment": "Automated build report"
                }'
            """, returnStdout: true).trim()

        // Check if the file was successfully uploaded
        def completeStatus = readJSON(text: completeResponse).ok
        if (!completeStatus) {
            error "Failed to complete file upload: ${completeResponse}"
        }
    }
}

