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


def call(String clusterName, String region, String targetVersion) {
    echo "Upgrading cluster to version: ${targetVersion}"
    
    def upgradeResult = sh(
      //  script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl upgrade cluster --name=${clusterName} --version=${targetVersion}  --region=${region} --approve",
        script: "/usr/local/bin/docker run --rm -v /Users/rahulgupta/.aws:/root/.aws public.ecr.aws/eksctl/eksctl upgrade cluster --name=eks-cluster --version=1.31 --region=ap-south-1 --approve",
        returnStdout: true
    ).trim()

    echo "Upgrade Result: ${upgradeResult}"

    if (upgradeResult.contains("cluster ${clusterName} control plane has been upgraded to version ${targetVersion}")) {
        return "true"
    } else {
        echo "Cluster upgrade failed"
        return "false"
    }
}






