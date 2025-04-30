import groovy.json.JsonSlurper

def call(String kubeconfig, String targetVersion) {
    def command = """
        /usr/local/bin/docker run --rm --network host \\
            -v ${kubeconfig}:/root/.kube/config \\
            -v ${System.getProperty("user.home")}/.aws:/root/.aws \\
            kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config
    """

    echo "📦 Running kubent check for deprecated Kubernetes APIs..."
    echo "📋 Command:\n${command}"

    def output = ''
    def status = 0

    try {
        output = sh(script: command, returnStdout: true).trim()
    } catch (err) {
        status = err.getCauses()?.first()?.getExitCode() ?: 1
        output = err.getMessage()
    }

    // Save output to file
    def fileName = 'kubent_output.json'
    writeFile file: fileName, text: output
    archiveArtifacts artifacts: fileName, onlyIfSuccessful: false

    def summary = ""
    if (status == 0) {
        echo "✅ PASS: No deprecated Kubernetes APIs found."
        summary = "No deprecated resources found."
        return [status: "PASS", summary: summary, json: output]
    } else {
        echo "❌ FAIL: Deprecated Kubernetes APIs detected."
        echo "📄 See 'kubent_output.json' for full details."

        // Try to parse JSON and count results
        try {
            def json = new JsonSlurper().parseText(output)
            def count = json instanceof List ? json.size() : 0
            summary = "${count} deprecated resource(s) found."
        } catch (parseErr) {
            summary = "Deprecated resources found, but JSON could not be parsed."
        }

        return [status: "FAIL", summary: summary, json: output]
    }
}
