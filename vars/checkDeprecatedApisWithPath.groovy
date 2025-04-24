def call(Map args) {
    String kubePath = args.kubePath
    String targetVersion = args.targetVersion.toString()
    String clusterInfo = args.clusterInfo

    echo "🔍 Running deprecated API check for cluster: ${clusterInfo}"

    def reportFileHtml = "kubent_failure_report_${clusterInfo}.html"
    def htmlContent = ""
    def kubentOutput = ""
    def kubentExitCode = 0

    // Run kubent and capture both exit code and output
    kubentExitCode = sh(
        script: """
            set +e
            OUTPUT=\$(/usr/local/bin/docker run --rm --network host \\
                -v ${kubePath}:/root/.kube/config \\
                -v ~/.aws:/root/.aws \\
                kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config)
            EXIT_CODE=\$?
            echo "\$OUTPUT" > kubent_output.json
            echo \$EXIT_CODE > kubent_exit_code.txt
            exit 0
        """,
        returnStatus: true
    )

    // Read output and exit code
    kubentOutput = readFile("kubent_output.json").trim()
    kubentExitCode = readFile("kubent_exit_code.txt").trim().toInteger()

    if (kubentExitCode == 0) {
        echo "✅ Kubent check passed. No deprecated APIs found or non-failing exit."
        return
    }

    // Start HTML
    htmlContent += """
        <html>
        <head><title>Kubent Failure Report</title></head>
        <body>
            <h1 style='color:red;'>Kubent Check Failed</h1>
            <p><strong>Cluster:</strong> ${clusterInfo}</p>
            <p><strong>Reason:</strong> An error occurred while running the Kubent check.</p>
            <p><strong>Exit Code:</strong> ${kubentExitCode}</p>
    """

    // Try to parse JSON output and render table
    try {
        def jsonResult = readJSON(text: kubentOutput)
        if (jsonResult && jsonResult.size() > 0) {
            htmlContent += """
                <h2>Detected Deprecated APIs</h2>
                <table border='1'>
                    <tr>
                        <th>Name</th><th>Namespace</th><th>Kind</th><th>API Version</th>
                        <th>Rule Set</th><th>Replace With</th><th>Since</th>
                    </tr>
            """
            jsonResult.each { item ->
                htmlContent += "<tr><td>${item.Name}</td><td>${item.Namespace}</td><td>${item.Kind}</td><td>${item.ApiVersion}</td><td>${item.RuleSet}</td><td>${item.ReplaceWith}</td><td>${item.Since}</td></tr>"
            }
            htmlContent += "</table>"
        } else {
            htmlContent += "<p>No deprecated APIs found in parsed JSON output.</p>"
        }
    } catch (e) {
        htmlContent += "<h2>Raw Output</h2><pre>${kubentOutput}</pre>"
    }

    // End HTML and write file
    htmlContent += "</body></html>"
    writeFile(file: reportFileHtml, text: htmlContent)

    publishHTML([
        reportName: "Kubent Failure Report",
        reportDir: ".",
        reportFiles: reportFileHtml
    ])

    echo "❗ Kubent check failed for cluster '${clusterInfo}'. Failure HTML report published."
}

