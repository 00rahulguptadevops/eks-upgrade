def call(String kubeconfig) {
    echo "validation kubernetes Cluster access using kubeconig ${kubeconfig}"
    def result = sh (
        script: """
         /user/local/bin/docker run --rm --network host \\
         -v ~/.aws/root/.aws \\
         -v ${kubeconfig}:/file -e KUBECONFIG=/file \\
         heyvaldemar/aws-kubectl kubectl cluster-info > /dev/null 2>$1 && echo "kubeconfig file: ${kubeconfig} is valid" || echo "kubeconfig file: ${kubeconfig} is Invalid" && exit 1
        """,
        returnStdout true

    ).trim()
    
    return result

}
