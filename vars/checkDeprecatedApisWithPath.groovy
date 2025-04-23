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

        if (jsonResult.size() > 0) {
            def reportFile = "deprecated_apis_report_${clusterInfo}.json"
            writeFile(file: reportFile, text: result)
            publishJSONReports(reports: reportFile)
            echo "❌ Deprecated APIs found in cluster '${clusterInfo}'. Report published."
        } else {
            echo "✅ No deprecated APIs found for cluster '${clusterInfo}'."
        }
    } catch (Exception e) {
        def failedFile = "kubent_check_failed_${clusterInfo}.json"
        writeFile(file: failedFile, text: '{"status": "failure", "message": "Kubent check failed"}')
        publishJSONReports(reports: failedFile)
        echo "❗ Kubent check failed for cluster '${clusterInfo}'. Report published."
    }
}

