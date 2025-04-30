import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def call(Map args = [:]) {
    def kubeconfig = args.kubeconfig ?: error("Missing 'kubeconfig' argument")
    def targetVersion = args.targetVersion ?: error("Missing 'targetVersion' argument")

    def outputFile = 'kubent_output.json'
    def exitCodeFile = 'kubent_exit_code.txt'

    // Run kubent and capture output and exit code
    def exitCode = sh(
      script: """
        /usr/local/bin/docker run --rm --network host \\
          -v ${kubeconfig}:/root/.kube/config \\
          -v ~/.aws:/root/.aws \\
          kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config > ${outputFile} 2>&1
      """,
      returnStatus: true
    )


    def output = readFile(outputFile).trim()
    exitCode = readFile(exitCodeFile).trim().toInteger()

    // Archive raw output
    writeFile file: 'kubent_output_raw.txt', text: output
    archiveArtifacts artifacts: 'kubent_output_raw.txt', onlyIfSuccessful: false

    def summary = ''
    def status = 'PASS'

    if (output.contains('[')) {
        try {
            def jsonPart = output.substring(output.indexOf('['))
            def data = new JsonSlurper().parseText(jsonPart)
            def count = data.size()

            if (count > 0) {
                summary = "❌ ${count} deprecated API(s) found."
                status = 'FAIL'

                // Write HTML report
                def html = """
                    <html>
                      <body>
                        <h2>Deprecated APIs</h2>
                        <pre>${JsonOutput.prettyPrint(jsonPart)}</pre>
                      </body>
                    </html>
                """
                writeFile file: 'kubent_report.html', text: html
                archiveArtifacts artifacts: 'kubent_report.html', onlyIfSuccessful: false
            } else {
                summary = "✅ PASS: No deprecated APIs found."
            }

        } catch (e) {
            echo "⚠️ Failed to parse JSON output: ${e.message}"
            summary = "⚠️ kubent output is not valid JSON."
        }
    } else {
        echo "⚠️ Output does not contain valid JSON array. Raw output shown above."
        summary = "⚠️ kubent did not produce JSON output."
    }

    // Handle result
    if (exitCode != 0) {
        echo summary
        error(summary)
    }

    echo summary
    return [status: status, summary: summary]
}
