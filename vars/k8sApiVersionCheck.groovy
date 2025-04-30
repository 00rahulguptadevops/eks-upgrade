def call(String kubeconfig, String targetVersion) {
    def outputFile = 'kubent_output.json'
    def summary = ''

    def exitCode = sh(
        script: """
            /usr/local/bin/docker run --rm --network host \\
              -v ${kubeconfig}:/root/.kube/config \\
              -v ~/.aws:/root/.aws \\
              kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config > ${outputFile} 2>&1
        """,
        returnStatus: true
    )

    def output = readFile(outputFile)
    writeFile file: outputFile, text: output
    archiveArtifacts artifacts: outputFile, allowEmptyArchive: true

    def reportLink = "${env.BUILD_URL}artifact/${outputFile}"

    if (exitCode != 0) {
        echo "❌ Deprecated APIs found!"
        echo "📄 JSON report: ${reportLink}"
        summary = "❌ FAIL: Deprecated APIs found.\n📄 Report: ${reportLink}"
        error(summary) // ⛔️ Fail the job
    } else {
        echo "✅ No deprecated APIs found."
        summary = "✅ API Check: No deprecated APIs found.\n📄 Report: ${reportLink}"
    }

    return [status: (exitCode == 0 ? 'PASS' : 'FAIL'), summary: summary]
}
