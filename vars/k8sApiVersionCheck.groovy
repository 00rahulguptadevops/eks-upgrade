def call(String kubeconfig, String targetVersion) {
    def result = sh (
        script: """
        /usr/local/bin/docker run --rm --network host \\
        -v ${kubeconfig}:/root/.kube/config \\
        -v ~/.aws:/root/.aws \\
        kubent:aws01 -t ${targetVersion} -o json -e -k /root/.kube/config
        """,
        returnStdout true
    ).trim()
}







                                sh """
                                    /usr/local/bin/docker run --rm --network host \\
                                      -v ${kubePath}:/root/.kube/config \\
                                      -v ~/.aws:/root/.aws \\
                                      kubent:aws01 -t ${clusterInfo.target_version} -o json -e -k /root/.kube/config
                                """