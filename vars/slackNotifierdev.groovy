def uploadFileToSlack(String filePath, String slackChannel) {
    withCredentials([string(credentialsId: 'slack-token', variable: 'SLACK_TOKEN')]) {
        if (!filePath || !fileExists(filePath)) {
            error "File does not exist: ${filePath}"
        }

        def fileSize = sh(script: "stat -c %s ${filePath}", returnStdout: true).trim()

        def uploadUrlResponse = sh(script: """
            curl -s -X POST https://slack.com/api/files.getUploadURLExternal \
                -H "Authorization: Bearer \$SLACK_TOKEN" \
                -H "Content-Type: application/json" \
                -d '{
                    "filename": "$(basename ${filePath})",
                    "length": ${fileSize},
                    "channels": ["${slackChannel}"]
                }'
            """, returnStdout: true).trim()

        def uploadUrl = readJSON(text: uploadUrlResponse).upload_url
        def fileId = readJSON(text: uploadUrlResponse).file_id

        if (!uploadUrl) {
            error "Failed to obtain upload URL from Slack: ${uploadUrlResponse}"
        }

        sh """
            curl -s -X POST ${uploadUrl} \
                -H "Authorization: Bearer \$SLACK_TOKEN" \
                -H "Content-Type: application/octet-stream" \
                --data-binary @${filePath}
        """

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

        def completeStatus = readJSON(text: completeResponse).ok
        if (!completeStatus) {
            error "Failed to complete file upload: ${completeResponse}"
        }
    }
}

