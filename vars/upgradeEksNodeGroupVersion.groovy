// def call(String clusterName, String nodegroupName, String region, String targeVersion) {
//     echo "Upgrading nodegroup '${nodegroupName}' in cluster '${clusterName}'..."

//     sh """
//     /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl upgrade nodegroup \
//       --cluster ${clusterName} \
//       --name ${nodegroupName} \
//       --region ${region} \
//       --kubernetes-version ${targeVersion} \
//       --wait=false
//     """

//     echo "✅ Upgrade complete for nodegroup '${nodegroupName}'"
// }


def call(String clusterName, String nodegroupName, String region, String targetVersion) {
    echo "Checking version for nodegroup '${nodegroupName}' in cluster '${clusterName}'..."

    // Get current version of the nodegroup
    def currentVersion = sh(
        script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl get nodegroup --cluster ${clusterName} --region ${region} -o json | jq -r '.[] | select(.Name==\"${nodegroupName}\") | .Version'",
        returnStdout: true
    ).trim()

    echo "🔍 Current version of '${nodegroupName}': ${currentVersion}"

    if (currentVersion == targetVersion) {
        echo "✅ Nodegroup '${nodegroupName}' is already at target version ${targetVersion}. Skipping upgrade."
        return "skipped"
    }

    try {
        echo "⬆️ Upgrading nodegroup '${nodegroupName}' to version ${targetVersion}..."

        sh """
        /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl upgrade nodegroup \
          --cluster ${clusterName} \
          --name ${nodegroupName} \
          --region ${region} \
          --kubernetes-version ${targetVersion} \
          --wait=false
        """

        echo "✅ Upgrade command completed for nodegroup '${nodegroupName}'"
        return "true"
    } catch (e) {
        echo "❌ Upgrade failed for nodegroup '${nodegroupName}': ${e.message}"
        return "false"
    }
}