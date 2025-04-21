def call(Map params) {
    String dockerImage = params.dockerImage
    String k8sTargetVersion = params.k8sTargetVersion
    String kubeconfigPath = params.kubeconfigPath
    String slackWebhookCredId = params.slackWebhookCredId

    String outputFile = 'deprecated-apis-output.json'

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

    writeFile file: outputFile, text: output

    if (output) {
        def jsonOutput = readJSON text: output
        if (jsonOutput && jsonOutput.size() > 0) {
            echo "❌ Deprecated APIs found."
            jsonOutput.each { item ->
                echo "- ${item.Kind} (${item.ApiVersion}) in namespace ${item.Namespace}, replace with: ${item.ReplaceWith}"
            }

            // Send JSON as a Slack code block
            def slackMessage = """*❌ Deprecated APIs Detected*\n\`\`\`${output}\`\`\`"""

            withCredentials([string(credentialsId: slackWebhookCredId, variable: 'SLACK_WEBHOOK')]) {
                httpRequest httpMode: 'POST',
                    url: "${SLACK_WEBHOOK}",
                    contentType: 'APPLICATION_JSON',
                    requestBody: """{
                        "text": "${slackMessage.replace('"', '\\"').replace('\n', '\\n')}"
                    }"""
            }

            archiveArtifacts artifacts: outputFile
            error("Deprecated APIs found. See Slack message and artifact.")
        }
    }

    echo "✅ No deprecated APIs detected."
}

