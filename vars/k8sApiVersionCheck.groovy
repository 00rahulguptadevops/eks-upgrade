import groovy.json.JsonSlurper

def call(Map args = [:]) {
    def kubeconfig = args.kubeconfig ?: error("Missing 'kubeconfig' argument")
    def targetVersion = args.targetVersion ?: error("Missing 'targetVersion' argument")

    def command = """
        /usr/local/bin/docker run --rm --network host \\
        -v ${kubeconfig}:/root/.kube/config \\
        -v ~/.aws:/root/.aws \\
        kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config
    """.stripIndent().trim()

    def output = ''
    def exitCode = 0

    try {
        output = sh(script: command, returnStdout: true).trim()
    } catch (err) {
        // Even if it "fails", we still get the output
        output = err.getMessage()
        exitCode = 200
    }

    // Always write and archive raw output
    writeFile file: 'kubent_output_raw.txt', text: output
    archiveArtifacts artifacts: 'kubent_output_raw.txt', onlyIfSuccessful: false

    def summary = ''
    def status = 'PASS'

    // Try parsing JSON if present
    if (output.contains('[')) {
        try {
            def jsonPart = output.substring(output.indexOf('['))
            def data = new JsonSlurper().parseText(jsonPart)
            def count = data.size()

            summary = "❌ ${count} deprecated API(s) found."
            status = 'FAIL'

            // Also write HTML report if needed
            def html = "<html><body><h2>Deprecated APIs</h2><pre>${groovy.json.JsonOutput.prettyPrint(jsonPart)}</pre></body></html>"
            writeFile file: 'kubent_report.html', text: html
            archiveArtifacts artifacts: 'kubent_report.html', onlyIfSuccessful: false

        } catch (e) {
            echo "⚠️ Failed to parse JSON output: ${e.message}"
            summary = "⚠️ kubent output is not valid JSON."
        }
    } else {
        echo "⚠️ Output does not contain valid JSON array. Raw output shown above."
        summary = "⚠️ kubent did not produce JSON output."
    }

    if (exitCode != 0) {
        echo summary
        error(summary)
    }

    echo "✅ PASS: No deprecated APIs found."
    return [status: status, summary: summary]
}
