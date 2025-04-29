def call(String clusterName, String region) {
    // Fetch current cluster version
    def clusterVersion = sh(script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl get cluster --name ${clusterName} --region ${region} -o json | jq -r '.[0].version'", returnStdout: true).trim()

    // Get nodegroups JSON data
    def nodegroupsJson = sh(script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl get nodegroup --cluster ${clusterName} --region ${region} -o json", returnStdout: true)
    def nodegroupVersions = []

    // Parse the nodegroups JSON response
    def nodegroups = readJSON text: nodegroupsJson

    // Iterate through nodegroups and capture all versions
    nodegroups.each { ng ->
        def ngName = ng.Name
        def ngVersion = ng.Version

        echo "Nodegroup '${ngName}' has version ${ngVersion}"

        nodegroupVersions << ngVersion  // Collect all node group versions
    }

    return nodegroupVersions  // Return all node group versions
}

