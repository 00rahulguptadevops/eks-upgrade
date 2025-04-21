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

    // Debug output to console
    echo "Kubent output: ${output}"

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

            // Debug Slack message before sending
            echo "Slack Message: ${slackMessage}"

            // Send Slack message
            slackSend(channel: slackChannel, message: slackMessage)
            error("Deprecated APIs detected.")
        } else {
            // If no deprecated APIs found, send success message
            def slackMessage = "✅ No deprecated APIs detected."
            echo "Slack Message: ${slackMessage}"
            slackSend(channel: slackChannel, message: slackMessage)
        }
    } else {
        // No output from the kubent command
        def slackMessage = "✅ No deprecated APIs detected."
        echo "Slack Message: ${slackMessage}"
        slackSend(channel: slackChannel, message: slackMessage)
    }
}

