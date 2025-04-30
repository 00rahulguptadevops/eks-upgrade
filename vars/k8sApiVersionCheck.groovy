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

    // Save and archive output
    def fileName = 'kubent_output.json'
    writeFile file: fileName, text: output
    archiveArtifacts artifacts: fileName, onlyIfSuccessful: false

    def summary = ""
    if (status == 0) {
        summary = "✅ PASS: No deprecated APIs found."
        echo summary
    } else {
        // Try to parse and summarize JSON
        try {
            def json = new JsonSlurper().parseText(output)
            def count = json instanceof List ? json.size() : 0
            summary = "❌ FAIL: ${count} deprecated API(s) found."
        } catch (parseErr) {
            summary = "❌ FAIL: Deprecated APIs detected (JSON parse error)."
        }

        echo summary
        echo "📄 See 'kubent_output.json' for details."
        error(summary) // Fail the pipeline step here
    }
}
