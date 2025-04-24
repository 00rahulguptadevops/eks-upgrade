def call(Map args) {
    String kubePath = args.kubePath
    String targetVersion = args.targetVersion.toString()
    String clusterInfo = args.clusterInfo

    echo "🔍 Running deprecated API check for cluster: ${clusterInfo}"

    def output = ''
    def exitCode = 0

    try {
        output = sh(
            script: """
                /usr/local/bin/docker run --rm --network host \\
                  -v ${kubePath}:/root/.kube/config \\
                  -v ~/.aws:/root/.aws \\
                  kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config
            """,
            returnStdout: true,
            returnStatus: true
        )
        exitCode = output.exitValue
        output = output.toString().trim()
    } catch (Exception e) {
        output = e.getMessage()
        exitCode = 1
    }

    def jsonPattern = /\[\s*{.*}\s*\]/s
    def jsonData = (output =~ jsonPattern) ? (output =~ jsonPattern)[0] : "[]"
    def jsonList = readJSON(text: jsonData)

    def htmlContent = """
    <html><body>
    <h1>Kubent Check Result</h1>
    <p><b>Cluster:</b> ${clusterInfo}</p>
    <p><b>Exit Code:</b> ${exitCode}</p>
    <p><b>Raw Output:</b></p>
    <pre>${output.encodeAsHTML()}</pre>
    """

    if (jsonList && jsonList.size() > 0) {
        htmlContent += "<h2>Deprecated APIs Found</h2><table border='1'><tr><th>Name</th><th>Namespace</th><th>Kind</th><th>API Version</th><th>RuleSet</th><th>Replace With</th><th>Since</th></tr>"
        jsonList.each { item ->
            htmlContent += "<tr><td>${item.Name}</td><td>${item.Namespace}</td><td>${item.Kind}</td><td>${item.ApiVersion}</td><td>${item.RuleSet}</td><td>${item.ReplaceWith}</td><td>${item.Since}</td></tr>"
        }
        htmlContent += "</table>"
    }

    htmlContent += "</body></html>"

    def htmlFile = "kubent_result_${clusterInfo}.html"
    writeFile file: htmlFile, text: htmlContent
    publishHTML([reportName: "Kubent Report - ${clusterInfo}", reportDir: ".", reportFiles: htmlFile])

    if (exitCode != 0) {
        error("❌ Kubent command failed for cluster ${clusterInfo}.")
    }

    if (jsonList && jsonList.size() > 0) {
        error("❌ Deprecated APIs detected for cluster ${clusterInfo}. Failing pipeline.")
    }

    echo "✅ No deprecated APIs found for cluster ${clusterInfo}."
}

