def call(String kubeconfig) {
    echo "🔍 Validating Kubernetes cluster access using kubeconfig: ${kubeconfig}"

    def exitCode = sh(
        script: """
            /usr/local/bin/docker run --rm --network host \\
              -v ~/.aws:/root/.aws \\
              -v ${kubeconfig}:/file -e KUBECONFIG=/file \\
              heyvaldemar/aws-kubectl sh -c 'kubectl cluster-info > /dev/null 2>&1'
        """,
        returnStatus: true
    )

    if (exitCode != 0) {
        def summary = ":x: Kubeconfig file `${kubeconfig}` is invalid"
        echo summary
        error("❌ Kubeconfig validation failed.\n${summary}")
    } else {
        def summary = ":white_check_mark: Kubeconfig file `${kubeconfig}` is valid"
        echo summary
        return [valid: true, summary: summary]
    }
}
