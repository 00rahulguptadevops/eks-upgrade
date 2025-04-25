// def call(String clusterName, String region, String targetVersion){
//     echo "Checking kubernetes version for eks cluster: ${clusterName} in region ${region}"

//     def currentVersion = sh(
//         script: """
//             /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl get cluster --name ${clusterName} --region ${region} -o json | jq -r '.[0].Version'
//             """,
//             returnStdout: true
//     ).trim()
//     echo "Cluster CurrentVersion: ${currentVersion}"
//     echo "Taget Version: ${targetVersion}"

//     if (currentVersion == targetVersion) {
//         echo "Cluster already running with ${targetVersion}"
//         return false //Indicate Skip
//     } else {
//       //  /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl upgrade cluster --name=${clusterName} --version=${targetVersion}  --region=${region} --approve
//        echo "Updating"
//         return true
//     }
// }


def checkAndUpgradeEksCluster(String clusterName, String region, String targetVersion) {
    // Fetch current cluster version (example using eksctl)
    def currentVersion = sh(
        script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl get cluster --name ${clusterName} --region ${region} -o json | jq -r '.[0].Version'",
        returnStdout: true
    ).trim()

    echo "Current version of the cluster: ${currentVersion}"

    if (currentVersion == targetVersion) {
        // If versions are the same, return "false" or "already_upgraded"
        echo "Cluster is already running the target version: ${targetVersion}"
        return "false"  // Indicates no upgrade is needed
    } else {
        // If versions are different, proceed with the upgrade
        echo "Upgrading cluster to version: ${targetVersion}"

        // Example command to upgrade the cluster (you can replace this with your actual upgrade logic)
        def upgradeResult = sh(
            script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl upgrade cluster --name=${clusterName} --version=${targetVersion}  --region=${region} --approve",
            returnStdout: true
        ).trim()

        if (upgradeResult.contains("Upgrade complete")) {
            echo "Cluster upgraded successfully"
            return "true"  // Indicates the upgrade was successful
        } else {
            echo "Cluster upgrade failed"
            return "false"  // Indicates the upgrade failed
        }
    }
}
