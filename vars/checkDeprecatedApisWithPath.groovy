def call(Map params) {
    String dockerImage = params.dockerImage
    String k8sTargetVersion = params.k8sTargetVersion
    String kubeconfigPath = params.kubeconfigPath
    String slackChannel = params.slackChannel

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
        if (jsonOutput) {
            def slackMessage = "❌ *Deprecated APIs found:*"
            jsonOutput.each { item ->
                slackMessage += "\n- *${item.Kind}* in namespace *${item.Namespace}* (API version: ${item.ApiVersion})"
                slackMessage += "\n  - Rule: ${item.RuleSet}"
                slackMessage += "\n  - Replace with: ${item.ReplaceWith}"
                slackMessage += "\n  - Since: ${item.Since}\n"
            }
            slackSend(channel: slackChannel, message: slackMessage)
            error("Deprecated APIs detected.")
        }
    }

    slackSend(channel: slackChannel, message: "✅ No deprecated APIs detected.")
    echo "✅ No deprecated APIs detected."
}

