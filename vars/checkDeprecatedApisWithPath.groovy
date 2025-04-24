def call(Map params) {
    def clusterName = params.clusterName
    def kubePath = params.kubePath
    def targetVersion = params.targetVersion ?: "1.32" // Default to 1.32 if not provided

    // Run kubent to check for deprecated APIs
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
    def output = result.split('---KUBENT_OUTPUT_START---')[1].split('---KUBENT_OUTPUT_END---')[0].trim()

    // Try to extract JSON array of deprecated APIs
    def jsonPattern = /\[\s*{.*}\s*]/  // match a list of JSON objects
    def matcher = (output =~ jsonPattern)
    def jsonData = matcher ? matcher[0] : "[]"
    def jsonList = readJSON text: jsonData

    // Ensure report folder exists
    def reportDir = "${env.WORKSPACE}/api-report"
    def reportFile = "${reportDir}/index.html"
    sh "mkdir -p ${reportDir}"

    // Create HTML report
    writeFile file: reportFile, text: """
        <html>
        <head><title>Kubent Check Report</title></head>
        <body>
        <h1>Kubent Check Report</h1>
        <p><strong>Cluster:</strong> ${clusterName}</p>
        <p><strong>Kubent Raw Output:</strong></p>
        <pre>${output}</pre>
        <p><strong>Deprecated APIs Detected:</strong></p>
        <pre>${groovy.json.JsonOutput.prettyPrint(jsonData)}</pre>
        </body>
        </html>
    """

    // Debug output to check if HTML file is created
    echo "Generated HTML content: ${readFile(reportFile)}"

    // Publish the HTML report to Jenkins
    publishHTML(target: [
        reportDir: reportDir,
        reportFiles: 'index.html',
        reportName: 'Kubent Failure Report',
        allowMissing: false,
        alwaysLinkToLastBuild: true,
        keepAll: true
    ])

    // Fail the build if deprecated APIs are found
    if (jsonList.size() > 0) {
        echo "❌ Deprecated APIs found in cluster '${clusterName}'. Failing pipeline."
        currentBuild.result = 'FAILURE' // Fail the build explicitly
        return
    }

    // If no deprecated APIs are found, pass the build
    echo "✅ No deprecated APIs found in cluster '${clusterName}'"
}

