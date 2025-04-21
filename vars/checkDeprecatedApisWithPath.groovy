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
            -t ${k8sTargetVersion} -o json -e -k /root/.kube/config 2>/dev/null
        """,
        returnStdout: true
    ).trim()

    if (output) {
        def jsonOutput = readJSON text: output
        if (jsonOutput && jsonOutput.size() > 0) {
            echo "❌ Deprecated APIs found."
            jsonOutput.each { item ->
                echo "- ${item.Kind} (${item.ApiVersion}) in namespace ${item.Namespace}, replace with: ${item.ReplaceWith}"
            }

            // Format message for Slack with Jenkins link
            def slackText = """
*❌ Deprecated APIs Detected*
\`\`\`
${output}
\`\`\`
🔗 *Job Link:* ${env.BUILD_URL}
"""
            def slackPayload = groovy.json.JsonOutput.toJson([text: slackText])

            withCredentials([string(credentialsId: slackWebhookCredId, variable: 'SLACK_WEBHOOK')]) {
                httpRequest(
                    httpMode: 'POST',
                    url: SLACK_WEBHOOK,
                    contentType: 'APPLICATION_JSON',
                    requestBody: slackPayload
                )
            }

            error("Deprecated APIs found. Slack notification sent.")
        }
    }

    echo "✅ No deprecated APIs detected."
}

