def call(Map args) {
    String kubePath = args.kubePath
    String targetVersion = args.targetVersion.toString()
    String clusterInfo = args.clusterInfo

    try {
        echo "🔍 Running deprecated API check for cluster: ${clusterInfo}"

        // Run kubent and capture stdout
        def result = sh(script: """
            /usr/local/bin/docker run --rm --network host \\
              -v ${kubePath}:/root/.kube/config \\
              -v ~/.aws:/root/.aws \\
              kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config
        """, returnStdout: true).trim()

        def jsonResult = readJSON(text: result)
        def reportFileJson = "deprecated_apis_report_${clusterInfo}.json"
        writeFile(file: reportFileJson, text: result)

        if (jsonResult && jsonResult.size() > 0) {
            // Generate HTML
            def reportFileHtml = "deprecated_apis_report_${clusterInfo}.html"
            def htmlContent = "<html><body><h1>Deprecated APIs Report for ${clusterInfo}</h1><table border='1'>"
            htmlContent += "<tr><th>Name</th><th>Namespace</th><th>Kind</th><th>API Version</th><th>RuleSet</th><th>ReplaceWith</th><th>Since</th></tr>"

            jsonResult.each { item ->
                htmlContent += "<tr><td>${item.Name}</td><td>${item.Namespace}</td><td>${item.Kind}</td><td>${item.ApiVersion}</td><td>${item.RuleSet}</td><td>${item.ReplaceWith}</td><td>${item.Since}</td></tr>"
            }

            htmlContent += "</table></body></html>"
            writeFile(file: reportFileHtml, text: htmlContent)

            // Publish HTML
            publishHTML([reportName: "Deprecated APIs Report", reportDir: ".", reportFiles: reportFileHtml])
            echo "❌ Deprecated APIs found in cluster '${clusterInfo}'. HTML report published."

            // Fail the pipeline
            error("Deprecated APIs detected in cluster '${clusterInfo}'. Failing pipeline.")
        } else {
            echo "✅ No deprecated APIs found for cluster '${clusterInfo}'."
        }

    } catch (Exception e) {
        def failedHtmlFile = "kubent_failed_${clusterInfo}.html"
        def failedJsonFile = "kubent_failed_${clusterInfo}.json"
        def errorOutput = e.getMessage()

        // Try extracting whatever was printed (even partial JSON) if available
        def fallbackOutput = errorOutput.find(/\[.*\]/) ?: "[]"
        writeFile(file: failedJsonFile, text: fallbackOutput)

        def htmlContent = """
        <html><body>
        <h1>Kubent Check Failed</h1>
        <p><b>Cluster:</b> ${clusterInfo}</p>
        <p><b>Reason:</b> An error occurred while running the Kubent check.</p>
        <p><b>Exception:</b> ${errorOutput}</p>
        <h2>Partial Output (if any)</h2>
        <pre>${fallbackOutput}</pre>
        </body></html>
        """
        writeFile(file: failedHtmlFile, text: htmlContent)

        publishHTML([reportName: "Kubent Failure Report", reportDir: ".", reportFiles: failedHtmlFile])
        echo "❗ Kubent check failed for cluster '${clusterInfo}'. Failure HTML report published."

        // Fail the pipeline
        error("Kubent execution failed for cluster '${clusterInfo}'.")
    }
}

