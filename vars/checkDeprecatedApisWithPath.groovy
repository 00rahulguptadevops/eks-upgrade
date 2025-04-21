def call(Map params) {
    String dockerImage = params.dockerImage
    String k8sTargetVersion = params.k8sTargetVersion
    String kubeconfigPath = params.kubeconfigPath
    String slackWebhookCredId = params.slackWebhookCredId

    def output = sh(
        script: """
            /usr/local/bin/docker run -i --rm \
            --network host \
            -v "${kubeconfigPath}:/root/.kube/config" \
            -v ~/.aws:/root/.aws \
            ${dockerImage} \
            -t ${k8sTargetVersion} -o json  -k /root/.kube/config 2>/dev/null
        """,
        returnStdout: true
    ).trim()

    echo "Output from Kubernetes API check:\n${output}" // Log the output in Jenkins job

    if (output) {
        def jsonOutput = readJSON text: output
        if (jsonOutput && jsonOutput.size() > 0) {
            echo "❌ Deprecated APIs found."

            // Save the JSON output to a file
            def jsonFile = "/tmp/deprecated_apis.json"
            writeFile file: jsonFile, text: output

            // Slack message
            def slackText = "*❌ Deprecated APIs Detected:*\n" +
                            "```\n${output}\n```\n" +
                            "🔗 *Job Link:* <${env.BUILD_URL}|View Failed Stage>"

            def slackPayload = groovy.json.JsonOutput.toJson([text: slackText])

            withCredentials([string(credentialsId: slackWebhookCredId, variable: 'SLACK_WEBHOOK')]) {
                // Send the Slack message
                httpRequest(
                    httpMode: 'POST',
                    url: SLACK_WEBHOOK,
                    contentType: 'APPLICATION_JSON',
                    requestBody: slackPayload
                )

                // Upload JSON file to Slack as an attachment
                def fileUploadPayload = [
                    file: new File(jsonFile).bytes,
                    filetype: 'json',
                    filename: 'deprecated_apis.json',
                    channels: slackWebhookCredId // Send to appropriate Slack channel
                ]
                
                // Upload file to Slack
                slackUploadFile(fileUploadPayload)
            }

            error("Deprecated APIs found. Slack message and file sent.")
        }
    }

    echo "✅ No deprecated APIs detected."
}

def slackUploadFile(Map payload) {
    // Function to upload a file to Slack
    httpRequest(
        httpMode: 'POST',
        url: 'https://slack.com/api/files.upload',
        headers: [
            'Authorization': "Bearer ${SLACK_TOKEN}"
        ],
        body: [
            file: payload.file,
            filetype: payload.filetype,
            filename: payload.filename,
            channels: payload.channels
        ]
    )
}

