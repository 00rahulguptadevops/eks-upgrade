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

    if (output) {
        def jsonOutput = readJSON text: output
        if (jsonOutput && jsonOutput.size() > 0) {
            echo "❌ Deprecated APIs found."

            // Convert the JSON output to a pretty string representation
            def jsonPretty = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(jsonOutput))

            def slackText = "*❌ Deprecated APIs Detected:*\n" +
                            "```\n${jsonPretty}\n```\n" +
                            "🔗 *Job Link:* <${env.BUILD_URL}|View Failed Stage>"

            def slackPayload = groovy.json.JsonOutput.toJson([text: slackText])

            withCredentials([string(credentialsId: slackWebhookCredId, variable: 'SLACK_WEBHOOK')]) {
                httpRequest(
                    httpMode: 'POST',
                    url: SLACK_WEBHOOK,
                    contentType: 'APPLICATION_JSON',
                    requestBody: slackPayload
                )
            }

            error("Deprecated APIs found. Slack message sent.")
        }
    }

    echo "✅ No deprecated APIs detected."
}

