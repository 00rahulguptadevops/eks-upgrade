import groovy.json.JsonSlurper

def call(String kubeconfig, String targetVersion) {
    def command = """
        /usr/local/bin/docker run --rm --network host \\
            -v ${kubeconfig}:/root/.kube/config \\
            -v ${System.getProperty("user.home")}/.aws:/root/.aws \\
            kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config
    """

    echo "📦 Running kubent check for deprecated Kubernetes APIs..."

    def output = ''
    def status = 0

    try {
        // Capture both stdout and stderr safely
        output = sh(script: "${command} 2>&1", returnStdout: true).trim()
        status = 0
    } catch (err) {
        output = err.getMessage()
        status = 1  // still continue to parse output
    }

    // Always archive raw output
    writeFile file: 'kubent_output.json', text: output
    archiveArtifacts artifacts: 'kubent_output.json', onlyIfSuccessful: false

    def isJson = output?.startsWith("[") || output?.startsWith("{")
    def summary = ''

    if (isJson) {
        try {
            def data = new JsonSlurper().parseText(output)
            def count = data.size()

            // Generate HTML report
            def html = """
            <html>
              <head>
                <title>Kubent Report</title>
                <style>
                  body { font-family: Arial, sans-serif; padding: 20px; }
                  table { border-collapse: collapse; width: 100%; }
                  th, td { border: 1px solid #ccc; padding: 8px; }
                  th { background-color: #f4f4f4; }
                </style>
              </head>
              <body>
                <h2>Kubent Deprecated API Report</h2>
                <table>
                  <tr>
                    <th>Name</th>
                    <th>Namespace</th>
                    <th>Kind</th>
                    <th>API Version</th>
                    <th>Rule Set</th>
                    <th>Replacement</th>
                    <th>Since</th>
                  </tr>
            """

            data.each { item ->
                html += """
                <tr>
                  <td>${item.Name}</td>
                  <td>${item.Namespace}</td>
                  <td>${item.Kind}</td>
                  <td>${item.ApiVersion}</td>
                  <td>${item.RuleSet}</td>
                  <td>${item.ReplaceWith}</td>
                  <td>${item.Since}</td>
                </tr>
                """
            }

            html += """
                </table>
              </body>
            </html>
            """

            writeFile file: 'kubent_report.html', text: html
            archiveArtifacts artifacts: 'kubent_report.html', onlyIfSuccessful: false
            echo "📄 HTML report generated and archived."

            if (count > 0) {
                summary = "❌ FAIL: ${count} deprecated APIs found."
                echo summary
                error(summary)  // Stop pipeline
            } else {
                summary = "✅ PASS: No deprecated APIs found."
                echo summary
            }
        } catch (err) {
            echo "⚠️ JSON parsed but failed to generate HTML: ${err.message}"
        }
    } else {
        echo "⚠️ kubent output is not valid JSON, skipping HTML generation."
        writeFile file: 'kubent_output.txt', text: output
        archiveArtifacts artifacts: 'kubent_output.txt', onlyIfSuccessful: false
        summary = "❌ FAIL: Invalid kubent output. Check logs and kubent_output.txt"
        error(summary)
    }

    return [status: (status == 0 ? 'PASS' : 'FAIL'), summary: summary]
}
