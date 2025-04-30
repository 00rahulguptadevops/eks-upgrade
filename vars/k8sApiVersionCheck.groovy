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
        status = 1
        output = err.getMessage()
    }

    // Save JSON output
    def jsonFile = 'kubent_output.json'
    writeFile file: jsonFile, text: output
    archiveArtifacts artifacts: jsonFile, onlyIfSuccessful: false

    // Generate HTML report
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
        echo "⚠️ Failed to generate HTML report: ${err.message}"
    }

    // Determine status
    def summary = ""
    if (status == 0) {
        summary = "✅ PASS: No deprecated APIs found."
        echo summary
    } else {
        try {
            def json = new JsonSlurper().parseText(output)
            def count = json instanceof List ? json.size() : 0
            summary = "❌ FAIL: ${count} deprecated API(s) found."
        } catch (parseErr) {
            summary = "❌ FAIL: Deprecated APIs detected (JSON parse error)."
        }

        echo summary
        echo "📄 See artifacts: kubent_output.json and kubent_report.html"
        error(summary) // Fail the job
    }

    return summary
}
