def call(String dockerImage, String k8sTargetVersion, String kubeconfigPath) {
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
            echo "❌ Deprecated APIs found:"
            jsonOutput.each { item ->
                echo "- ${item.kind} (${item.apiversion}) in namespace ${item.namespace}, replace with: ${item.replacement}"
            }
            error("Deprecated APIs detected.")
        }
    }

    echo "✅ No deprecated APIs detected."
}

