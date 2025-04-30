def call(String kubeconfig, String targetVersion) {
    def outputFile = 'kubent_output.json'
    def rawFile = 'kubent_output_raw.txt'

    def command = """
        /usr/local/bin/docker run --rm --network host \\
            -v ${kubeconfig}:/root/.kube/config \\
            -v ~/.aws:/root/.aws \\
            kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config
    """

    def output = sh(script: command, returnStdout: true).trim()

    // Save raw output
    writeFile(file: rawFile, text: output)
    archiveArtifacts artifacts: rawFile, allowEmptyArchive: true

    def data = []
    def summary = ""
    def status = "PASS"

    try {
        def jsonStart = output.indexOf('[')
        if (jsonStart < 0) {
            throw new Exception("No JSON array found")
        }

        def jsonPart = output.substring(jsonStart)
        data = new groovy.json.JsonSlurper().parseText(jsonPart)
        writeFile(file: outputFile, text: groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(data)))
        archiveArtifacts artifacts: outputFile, allowEmptyArchive: true

        def jobName = env.JOB_NAME ?: 'job'
        def buildNum = env.BUILD_NUMBER ?: 'lastSuccessfulBuild'
        def baseUrl = env.BUILD_URL ?: "https://jenkins.example.com/job/${jobName}/${buildNum}/"
        def reportUrl = "${baseUrl}artifact/${outputFile}"

        if (data.size() > 0) {
            status = "FAIL"
            summary = "❌ FAIL: ${data.size()} deprecated API(s) found.\n📄 Report: ${reportUrl}"
            echo summary
            error(summary)
        } else {
            summary = "✅ PASS: No deprecated APIs found.\n📄 Report: ${reportUrl}"
            echo summary
        }

    } catch (Exception e) {
        summary = "⚠️ Failed to parse kubent output or generate report: ${e.message}"
        echo summary
        error(summary)
    }

    return [status: status, summary: summary]
}
