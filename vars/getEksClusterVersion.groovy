def call(String clusterName, String region) {
    def currentVersion = sh(
        script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl get cluster --name ${clusterName} --region ${region} -o json | jq -r '.[0].Version'",
        returnStdout: true
    ).trim()

    echo "Current version of the cluster: ${currentVersion}"
    

    if (currentVersion == clusterInfo.target_version) {
        echo "Cluster is already at the target version: ${clusterInfo.target_version}. Skipping upgrade."
        currentBuild.result = 'SUCCESS'
        return  // exit stage early
    }  
    return currentVersion
}


    