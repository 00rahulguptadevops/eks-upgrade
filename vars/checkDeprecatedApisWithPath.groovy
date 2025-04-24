def call(Map args) {
    String kubePath = args.kubePath
    String targetVersion = args.targetVersion.toString()
    String clusterInfo = args.clusterInfo

    try {
        echo "🔍 Running deprecated API check for cluster: ${clusterInfo}"

        def result = sh(
            script: """
                /usr/local/bin/docker run --rm --network host \\
                  -v ${kubePath}:/root/.kube/config \\
                  -v ~/.aws:/root/.aws \\
                  kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config
            """,
            returnStdout: true
        ).trim()

        def jsonResult = readJSON(text: result)
        def baseFileName = "kubent_report_${clusterInfo}"
        def reportFileJson = "${baseFileName}.json"
        def reportFileHtml = "${baseFileName}.html"

        if (jsonResult.size() > 0) {
            writeFile(file: reportFileJson, text: result)

            def htmlContent = """
                <html>
                <head><title>Deprecated APIs Report</title></head>
                <body>
                <h1>Deprecated APIs Report for ${clusterInfo}</h1>
                <table border='1'>
                <tr>
                    <th>Name</th><th>Namespace</th><th>Kind</th><th>API Version</th>
                    <th>Rule Set</th><th>Replace With</th><th>Deprecated Since</th>
                </tr>
            """

            jsonResult.each { item ->
                htmlContent += "<tr><td>${item.Name}</td><td>${item.Namespace}</td><td>${item.Kind}</td><td>${item.ApiVersion}</td><td>${item.RuleSet}</td><td>${item.ReplaceWith}</td><td>${item.Since}</td></tr>"
            }

            htmlContent += "</table></body></html>"
            writeFile(file: reportFileHtml, text: htmlContent)

            publishHTML([
                reportName: "Kubent Report",
                reportDir: ".",
                reportFiles: reportFileHtml
            ])

            echo "❌ Deprecated APIs found in cluster '${clusterInfo}'. HTML report published."
        } else {
            echo "✅ No deprecated APIs found for cluster '${clusterInfo}'."
        }

    } catch (e) {
        echo "⚠️ Kubent check failed: ${e.message}"

        // Try to capture any partial output
        def output = ""
        try {
            output = sh(
                script: """
                    /usr/local/bin/docker run --rm --network host \\
                      -v ${kubePath}:/root/.kube/config \\
                      -v ~/.aws:/root/.aws \\
                      kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config
                """,
                returnStdout: true,
                returnStatus: false
            ).trim()
        } catch (ignore) {
            // intentionally ignore nested exception
        }

        def failedFileHtml = "kubent_failure_${clusterInfo}.html"
        def html = """
            <html>
            <head><title>Kubent Failure Report</title></head>
            <body>
                <h1 style='color:red;'>Kubent Check Failed</h1>
                <p><strong>Cluster:</strong> ${clusterInfo}</p>
                <p><strong>Reason:</strong> An error occurred while running the Kubent check.</p>
                <p><strong>Exception:</strong> ${e.getMessage()}</p>
        """

        // Try parsing output as JSON
        if (output?.startsWith("[") || output?.startsWith("{")) {
            try {
                def fallbackJson = readJSON(text: output)
                html += """
                    <h2>Partial Output</h2>
                    <table border='1'>
                        <tr>
                            <th>Name</th><th>Namespace</th><th>Kind</th><th>API Version</th>
                            <th>Rule Set</th><th>Replace With</th><th>Deprecated Since</th>
                        </tr>
                """
                fallbackJson.each { item ->
                    html += "<tr><td>${item.Name}</td><td>${item.Namespace}</td><td>${item.Kind}</td><td>${item.ApiVersion}</td><td>${item.RuleSet}</td><td>${item.ReplaceWith}</td><td>${item.Since}</td></tr>"
                }
                html += "</table>"
            } catch (parseEx) {
                html += "<pre>${output}</pre>"
            }
        } else if (output) {
            html += "<h2>Raw Output</h2><pre>${output}</pre>"
        }

        html += "</body></html>"
        writeFile(file: failedFileHtml, text: html)

        publishHTML([
            reportName: "Kubent Report",
            reportDir: ".",
            reportFiles: failedFileHtml
        ])

        echo "❗ Kubent check failed for cluster '${clusterInfo}'. Failure HTML report published."
    }
}

