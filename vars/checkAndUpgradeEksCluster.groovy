def call(String clusterName, String region, String targetVersion){
    echo "Checking kubernetes version for eks cluster: ${clusterName} in region ${region}"

    def currentVersion = sh(
        script: """
            /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl get cluster --name ${clusterName} --region ${region} -o json | jq -r '.[0].Version'
            """,
            returnStdout: true
    ).trim()
    echo "Cluster CurrentVersion: ${currentVersion}"
    echo "Taget Version: ${targetVersion}"

    if (currentVersion == targetVersion) {
        echo "Cluster already running with ${targetVersion}"
        return false //Indicate Skip
    } else {
        echo "Upgrading Cluster ${targetVersion}"
        return true
    }
}