def call(String clusterName, String region) {
    def nodegroupJson = sh(
        script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl get nodegroup --cluster ${clusterName} --region ${region} -o json",
        returnStdout: true
    )

    def nodegroups = readJSON text: nodegroupJson

    def result = [:]
    nodegroups.each { ng ->
        result[ng.Name] = ng.Version
        echo "🔍 Nodegroup '${ng.Name}' is currently on version: ${ng.Version}"
    }

    return result
}
