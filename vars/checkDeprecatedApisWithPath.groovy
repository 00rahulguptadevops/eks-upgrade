def call(Map args) {
    String kubePath = args.kubePath
    String targetVersion = args.targetVersion.toString()
    String clusterInfo = args.clusterInfo

    echo "🔍 Running deprecated API check for cluster: ${clusterInfo}"

    def result = sh(
        script: """
            set +e
            OUTPUT=\$(/usr/local/bin/docker run --rm --network host \\
                -v ${kubePath}:/root/.kube/config \\
                -v ~/.aws:/root/.aws \\
                kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config)
            STATUS=\$?
            echo "---KUBENT_OUTPUT_START---"
            echo "\$OUTPUT"
            echo "---KUBENT_OUTPUT_END---"
            exit \$STATUS
        """,
        returnStdout: true
    ).trim()

    // Extract output between markers
    def output = (result =~ /---KUBENT_OUTPUT_START---(.*)---KUBENT_OUTPUT_END---/s)[0][1].trim()
    def jsonPattern = /\[\s*{.*?}\s*]/s
    def matcher = (output =~ jsonPattern)
    def jsonData = matcher ? matcher[0] : "[]"
    def jsonList = readJSON text: jsonData

    // Generate HTML
    def htmlContent = """
    <html><body>
    <h1>Kubent Check Report</h1>
    <p><b>Cluster:</b> ${clusterInfo}</p>
    <h2>Raw Output</h2>
    <pre>${output.encodeAsHTML()}</pre>
    """

    if (jsonList && jsonList.size() > 0) {
        htmlContent += """
        <h2>Deprecated APIs Detected</h2>
        <table border="1" cellpadding="4" cellspacing="0">
            <tr>
                <th>Name</th><th>Namespace</th><th>Kind</th>
                <th>API Version</th><th>RuleSet</th>
                <th>Replace With</th><th>Since</th>
            </tr>
        """
        jsonList.each { item ->
            htmlContent += """
            <tr>
                <td>${item.Name}</td><td>${item.Namespace}</td><td>${item.Kind}</td>
                <td>${item.ApiVersion}</td><td>${item.RuleSet}</td>
                <td>${item.ReplaceWith}</td><td>${item.Since}</td>
            </tr>
            """
        }
        htmlContent += "</table>"
    } else {
        htmlContent += "<p>No deprecated APIs found.</p>"
    }

    htmlContent += "</body></html>"

    def htmlFile = "kubent_report_${clusterInfo}.html"
    writeFile file: htmlFile, text: htmlContent
    publishHTML([reportName: "Kubent Report - ${clusterInfo}", reportDir: ".", reportFiles: htmlFile])

    // Fail the job if deprecated APIs are found
    if (jsonList && jsonList.size() > 0) {
        error("❌ Deprecated APIs detected in cluster ${clusterInfo}. Failing pipeline.")
    }

    echo "✅ No deprecated APIs found for cluster ${clusterInfo}."
}

