def call(String dockerImage, String k8sTargetVersion, String kubeconfigPath, String slackChannel) {
    // Run kubent command to check for deprecated APIs
    def result = sh(
        script: """
            /usr/local/bin/docker run -i --rm \
            --network host \
            -v "${kubeconfigPath}:/root/.kube/config" \
            -v ~/.aws:/root/.aws \
            ${dockerImage} \
            -t ${k8sTargetVersion} -o json -e -k /root/.kube/config 2>/dev/null
        """,
        returnStdout: true,
        returnStatus: true // Capture the exit code as well
    )

    // Capture exit code and stdout separately
    def exitCode = result.returnStatus
    def output = result.stdout

    // Check if the output is a valid string
    if (output instanceof String) {
        output = output.trim() // Trim only if it's a string
    } else {
        echo "Error: Expected string output from kubent but got an integer or other type."
    }

    // Debug: Output the kubent result
    echo "Kubent output: ${output}"

    // If there is any output (i.e., deprecated APIs detected)
    if (output) {
        def jsonOutput = readJSON text: output
        if (jsonOutput) {
            def slackMessage = "❌ *Deprecated APIs found:*"

            // Loop through the deprecated APIs and prepare the Slack message
            jsonOutput.each { item ->
                slackMessage += "\n- *${item.Kind}* in namespace *${item.Namespace}* (API version: ${item.ApiVersion})"
                slackMessage += "\n  - Rule: ${item.RuleSet}"
                slackMessage += "\n  - Replace with: ${item.ReplaceWith}"
                slackMessage += "\n  - Since: ${item.Since}"
                slackMessage += "\n"
            }

            // Send message to Slack channel
            slackSend(channel: slackChannel, message: slackMessage)
            echo "⚠️ Deprecated APIs detected, but pipeline will not fail."
        }
    } else {
        // If no deprecated APIs are detected
        def slackMessage = "✅ No deprecated APIs detected."
        slackSend(channel: slackChannel, message: slackMessage)
    }

    // Optional: If you need to write the output to a file (ensure it's a string)
    if (output instanceof String) {
        writeFile(file: 'kubent_output.json', text: output) // Writing output to file
    } else {
        echo "Error: Unable to write non-string output to file."
    }

    // Handle non-zero exit code (failure in kubent command)
    if (exitCode != 0) {
        echo "Error: kubent command failed with exit code ${exitCode}"
        error("kubent command failed.")
    }
}

