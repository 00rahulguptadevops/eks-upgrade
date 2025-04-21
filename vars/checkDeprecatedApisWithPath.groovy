def call(String dockerImage, String k8sTargetVersion, String kubeconfigPath, String slackChannel) {
    // Run kubent command to check for deprecated APIs
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

    // If there are deprecated APIs found
    if (output) {
        def jsonOutput = readJSON text: output
        if (jsonOutput) {
            def slackMessage = "❌ *Deprecated APIs found:*"

            // Construct Slack message for each deprecated API
            jsonOutput.each { item ->
                slackMessage += "\n- *${item.Kind}* in namespace *${item.Namespace}* (API version: ${item.ApiVersion})"
                slackMessage += "\n  - Rule: ${item.RuleSet}"
                slackMessage += "\n  - Replace with: ${item.ReplaceWith}"
                slackMessage += "\n  - Since: ${item.Since}"
                slackMessage += "\n"
            }

            // Send Slack message
            slackSend(channel: slackChannel, message: slackMessage)
            error("Deprecated APIs detected.")
        }
    } else {
        def slackMessage = "✅ No deprecated APIs detected."
        slackSend(channel: slackChannel, message: slackMessage)
    }
}

