def call(Map args) {
    String kubePath = args.kubePath
    String targetVersion = args.targetVersion.toString()
    String clusterInfo = args.clusterInfo

    try {
        echo "🔍 Running deprecated API check for cluster: ${clusterInfo}"

        // Run the Kubent check to get the deprecated API results in JSON format
        def result = sh(script: """
            /usr/local/bin/docker run --rm --network host \\
              -v ${kubePath}:/root/.kube/config \\
              -v ~/.aws:/root/.aws \\
              kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config
        """, returnStdout: true).trim()

        // Parse the JSON result
        def jsonResult = readJSON(text: result)

        // Check if deprecated APIs were found
        if (jsonResult.size() > 0) {
            def reportFileJson = "deprecated_apis_report_${clusterInfo}.json"
            writeFile(file: reportFileJson, text: result)

            // Convert JSON to HTML format
            def reportFileHtml = "deprecated_apis_report_${clusterInfo}.html"
            def htmlContent = "<html><body><h1>Deprecated APIs Report for ${clusterInfo}</h1><table border='1'>"
            htmlContent += "<tr><th>Resource</th><th>API</th><th>Version</th><th>Deprecated</th></tr>"

            // Loop through the JSON result and generate HTML table rows
            jsonResult.each { api ->
                api.resources.each { resource ->
                    htmlContent += "<tr><td>${resource.name}</td><td>${api.name}</td><td>${api.version}</td><td>${api.deprecated}</td></tr>"
                }
            }
            htmlContent += "</table></body></html>"

            // Write the HTML content to a file
            writeFile(file: reportFileHtml, text: htmlContent)

            // Publish the HTML report to Jenkins
            publishHTML([reportName: "Deprecated APIs Report", reportDir: ".", reportFiles: reportFileHtml])

            echo "❌ Deprecated APIs found in cluster '${clusterInfo}'. HTML report published."
        } else {
            echo "✅ No deprecated APIs found for cluster '${clusterInfo}'."
        }
    } catch (Exception e) {
        def failedFile = "kubent_check_failed_${clusterInfo}.json"
        writeFile(file: failedFile, text: '{"status": "failure", "message": "Kubent check failed"}')

        // Publish failure JSON report (if needed)
        writeFile(file: failedFile, text: '{"status": "failure", "message": "Kubent check failed"}')
        publishJSONReports(reports: failedFile) // Remove this line if it's not needed.

        echo "❗ Kubent check failed for cluster '${clusterInfo}'. JSON report published."
    }
}

