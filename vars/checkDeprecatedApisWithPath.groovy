def call(Map params) {
    String dockerImage = params.dockerImage
    String k8sTargetVersion = params.k8sTargetVersion
    String kubeconfigPath = params.kubeconfigPath
    String slackChannel = params.slackChannel ?: '#general'  // fallback to default

    // Temp output file
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

    // Save the output to a file
    writeFile file: outputFile, text: output

    if (output) {
        def jsonOutput = readJSON text: output
        if (jsonOutput && jsonOutput.size() > 0) {
            def slackMessage = "*❌ Deprecated APIs detected in cluster:*\n"
            jsonOutput.each { item ->
                slackMessage += "*Name:* ${item.Name}\n"
                slackMessage += "*Namespace:* ${item.Namespace}\n"
                slackMessage += "*Kind:* ${item.Kind}\n"
                slackMessage += "*API Version:* ${item.ApiVersion}\n"
                slackMessage += "*Rule:* ${item.RuleSet}\n"
                slackMessage += "*Replace With:* ${item.ReplaceWith}\n"
                slackMessage += "*Since:* ${item.Since}\n"
                slackMessage += "-----------------------------\n"
            }

            // Send Slack message with file content
            slackSend(channel: slackChannel, message: slackMessage)

            // Optionally, archive file in Jenkins
            archiveArtifacts artifacts: outputFile

            // Fail pipeline
            error("Deprecated APIs found. See Slack message and archived output.")
        }
    }

    slackSend(channel: slackChannel, message: "✅ No deprecated APIs detected.")
    echo "✅ No deprecated APIs detected."
}

