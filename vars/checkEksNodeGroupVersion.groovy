def call(String clusterName, String region) {
    def clusterVersion = sh(script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl get cluster --name ${clusterName} --region ${region} -o json | jq -r '.[0].version'", returnStdout: true).trim()

    echo "Cluster Version: ${clusterVersion}"

    def nodegroupsJson = sh(script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl get nodegroup --cluster ${clusterName} --region ${region} -o json", returnStdout: true)
    def outdatedNodeGroups = []

    def nodegroups = readJSON text: nodegroupsJson

    nodegroups.each { ng ->
        def ngName = ng.Name
        def ngVersion = ng.Version

        if (ngVersion < clusterVersion) {
            echo "⬆️ Nodegroup '${ngName}' can be upgraded from ${ngVersion} to ${clusterVersion}"
            outdatedNodeGroups << ngName
        } else {
            echo "✅ Nodegroup '${ngName}' is up to date (${ngVersion})"
        }
    }

    return outdatedNodeGroups
}
