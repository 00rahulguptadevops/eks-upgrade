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
        status = 0
    } catch (err) {
        output = err.getMessage()
        status = 1  // Assume failure
    }

    // Save raw output as JSON file
    def jsonFile = 'kubent_output.json'
    writeFile file: jsonFile, text: output
    archiveArtifacts artifacts: jsonFile, onlyIfSuccessful: false

    // Try parsing the JSON to build an HTML report
    try {
        def data = new JsonSlurper().parseText(output)
        def html = """
        <html>
          <head>
            <title>Kubent Report</title>
            <style>
              body { font-family: Arial, sans-serif; padding: 20px; }
              table { border-collapse: collapse; width: 100%; }
              th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
              th { background-color: #f2f2f2; }
            </style>
          </head>
          <body>
            <h2>Kubent Deprecated API Report</h2>
            <table>
              <tr>
                <th>Kind</th>
                <th>Name</th>
                <th>Namespace</th>
                <th>API Version</th>
                <th>Replacement</th>
              </tr>
        """

        data.each { item ->
            html += """
            <tr>
              <td>${item.kind}</td>
              <td>${item.name}</td>
              <td>${item.namespace}</td>
              <td>${item.apiVersion}</td>
              <td>${item.replacementAPI}</td>
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
    } catch (err) {
        echo "⚠️ Failed to parse kubent output or generate HTML: ${err.message}"
    }

    // Final status and summary
    def summary = ''
    if (status == 0) {
        summary = "✅ PASS: No deprecated APIs found."
        echo summary
    } else {
        def count = 0
        try {
            def parsed = new JsonSlurper().parseText(output)
            if (parsed instanceof List) {
                count = parsed.size()
            }
        } catch (ignored) { /* fallback to generic error */ }

        summary = "❌ FAIL: ${count} deprecated API(s) found."
        echo summary
        echo "📄 See kubent_output.json and kubent_report.html for details."
        error(summary) // fail the build step
    }

    return [status: (status == 0 ? 'PASS' : 'FAIL'), summary: summary]
}
