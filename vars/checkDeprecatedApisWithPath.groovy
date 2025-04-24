def call(Map params) {
    def clusterName = params.clusterName
    def kubePath = params.kubePath
    def targetVersion = params.targetVersion ?: "1.32" // Default to 1.32 if not provided

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

    // ✅ Extract only the output between markers using split (Groovy-compatible)
    def output = result.split('---KUBENT_OUTPUT_START---')[1].split('---KUBENT_OUTPUT_END---')[0].trim()

    // ✅ Try to extract JSON array of deprecated APIs
    def jsonPattern = /\[\s*{.*}\s*]/  // match a list of JSON objects
    def matcher = (output =~ jsonPattern)
    def jsonData = matcher ? matcher[0] : "[]"
    def jsonList = readJSON text: jsonData

    def reportFile = "api-report/index.html"
    def reportDir = "api-report"

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

    publishHTML(target: [
        reportDir           : reportDir,
        reportFiles         : 'index.html',
        reportName          : 'Kubent Failure Report',
        allowMissing        : false,
        alwaysLinkToLastBuild: true,
        keepAll             : true
    ])

    // If deprecated APIs are found, fail the job
    if (jsonList.size() > 0) {
        error("❌ Deprecated APIs found in cluster '${clusterName}'. Failing pipeline.")
    }

    echo "✅ No deprecated APIs found in cluster '${clusterName}'"
}

