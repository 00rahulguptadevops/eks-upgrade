def call(Map args) {
    String kubePath = args.kubePath
    String targetVersion = args.targetVersion.toString()
    String clusterInfo = args.clusterInfo

    try {
        echo "🔍 Running deprecated API check for cluster: ${clusterInfo}"

        def result = sh(script: """
            /usr/local/bin/docker run --rm --network host \\
              -v ${kubePath}:/root/.kube/config \\
              -v ~/.aws:/root/.aws \\
              kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config
        """, returnStdout: true).trim()

        def jsonResult = readJSON(text: result)

        // Convert JSON to HTML
        def htmlReport = convertJsonToHtml(jsonResult, clusterInfo)

        // Write the HTML report to a file
        def htmlReportFile = "deprecated_apis_report_${clusterInfo}.html"
        writeFile(file: htmlReportFile, text: htmlReport)

        // Archive the HTML report as an artifact
        archiveArtifacts artifacts: htmlReportFile, allowEmptyArchive: true

        // Publish the HTML report using the HTML Publisher plugin
        publishHTML(target: [
            reportName: "Deprecated APIs Report",
            reportDir: ".",
            reportFiles: htmlReportFile,
            keepAll: true
        ])

        echo "❌ Deprecated APIs found in cluster '${clusterInfo}'. HTML report published."
    } catch (Exception e) {
        def failedFile = "kubent_check_failed_${clusterInfo}.html"
        writeFile(file: failedFile, text: "<html><body><h1>Kubent check failed for cluster '${clusterInfo}'.</h1></body></html>")

        // Archive the failure HTML report
        archiveArtifacts artifacts: failedFile, allowEmptyArchive: true

        // Publish failure HTML report
        publishHTML(target: [
            reportName: "Kubent Check Failed",
            reportDir: ".",
            reportFiles: failedFile,
            keepAll: true
        ])

        echo "❗ Kubent check failed for cluster '${clusterInfo}'. HTML report published."
    }
}

// Convert JSON to HTML table
def convertJsonToHtml(def jsonResult, String clusterInfo) {
    def html = """
    <html>
        <head><title>Deprecated APIs Report for ${clusterInfo}</title></head>
        <body>
            <h1>Deprecated APIs Report for ${clusterInfo}</h1>
            <table border="1">
                <tr>
                    <th>API Version</th>
                    <th>Kind</th>
                    <th>Deprecated Version</th>
                </tr>
    """
    
    // Iterate over the JSON result and create a table row for each item
    jsonResult.each { item ->
        html += """
        <tr>
            <td>${item.apiVersion}</td>
            <td>${item.kind}</td>
            <td>${item.deprecatedVersion}</td>
        </tr>
        """
    }

    html += """
            </table>
        </body>
    </html>
    """
    return html
}

