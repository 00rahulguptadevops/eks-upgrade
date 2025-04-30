def call(String kubeconfig) {
    echo "Validating Kubernetes cluster access using kubeconfig: ${kubeconfig}"

    def result = sh(
        script: """
            /usr/local/bin/docker run --rm --network host \\
              -v ~/.aws:/root/.aws \\
              -v ${kubeconfig}:/file -e KUBECONFIG=/file \\
              heyvaldemar/aws-kubectl sh -c '(kubectl cluster-info > /dev/null 2>&1 && echo "true") || (echo "false" && exit 1)'
        """,
        returnStdout: true
    ).trim()
     
    echo result

    def summary = result == "true"
        ? ":white_check_mark: Kubeconfig file `${kubeconfig}` is valid\n"
        : ":x: Kubeconfig file `${kubeconfig}` is invalid\n"

    return [valid: result == "true", summary: summary]
}
