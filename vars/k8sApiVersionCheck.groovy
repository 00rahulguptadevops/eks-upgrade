import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def call(String kubeconfig, String targetVersion) {
    def command = """
        /usr/local/bin/docker run --rm --network host \\
            -v ${kubeconfig}:/root/.kube/config \\
            -v ${System.getProperty("user.home")}/.aws:/root/.aws \\
            kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config
    """

    echo "📦 Running kubent check for deprecated Kubernetes APIs..."

    def output = ''
    try {
        output = sh(script: "${command} 2>&1", returnStdout: true).trim()
    } catch (err) {
        output = err.getMessage()
    }

    // Always save raw output
    writeFile file: 'kubent_output_raw.txt', text: output
    archiveArtifacts artifacts: 'kubent_output_raw.txt', onlyIfSuccessful: false

    def summary = ''
    try {
        def jsonPart = output.substring(output.indexOf('['))
        def data = new JsonSlurper().parseText(jsonPart)
        def count = data.size()

        // Save cleaned JSON
        def prettyJson = JsonOutput.prettyPrint(JsonOutput.toJson(data))
        writeFile file: 'kubent_clean.json', text: prettyJson
        archiveArtifacts artifacts: 'kubent_clean.json', onlyIfSuccessful: false

        // Generate HTML report
        def html = """
        <html>
          <head>
            <title>Kubent Report</title>
            <style>
              body { font-family: Arial; padding: 20px; }
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
            error(summary)
        } else {
            summary = "✅ PASS: No deprecated APIs found."
            echo summary
        }

    } catch (Exception ex) {
        echo "⚠️ Failed to parse JSON output: ${ex.message}"
        error("❌ Unable to parse kubent output. Check kubent_output_raw.txt")
    }

    return [status: 'PASS', summary: summary]
}
