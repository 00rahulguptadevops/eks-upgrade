def call(String kubeconfig, String targetVersion) {
    def outputFile = 'kubent_output_raw.txt'

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

    // Archive raw output
    writeFile file: 'kubent_output.json', text: output
    archiveArtifacts artifacts: 'kubent_output.json', allowEmptyArchive: true

    if (exitCode != 0) {
        echo "❌ FAIL: Deprecated APIs found."
        echo output.take(500)  // Show trimmed output in console
        error("Deprecated APIs present. Please upgrade before continuing.")
    } else {
        echo "✅ PASS: No deprecated APIs found."
    }
}
