def call(String clusterName, String region, String targetVersion) {
    echo "Checking current version of cluster '${clusterName}' in region '${region}'..."

    def currentVersion = sh(
        script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl get cluster --name ${clusterName} --region ${region} -o json | jq -r '.[0].Version'",
        returnStdout: true
    ).trim()

    echo "Current version of the cluster: ${currentVersion}"

    if (currentVersion == targetVersion) {
        echo "ℹ️ Cluster '${clusterName}' is already at version ${targetVersion} in region ${region}. Skipping upgrade."
        return
    }

    try {
        timeout(time: 5, unit: 'MINUTES') {
            input message: "Approve upgrade of cluster '${clusterName}' from ${currentVersion} to ${targetVersion}?"
        }
    } catch (err) {
        error "❌ Upgrade was cancelled or timed out for cluster '${clusterName}' in region ${region}."
    }

    echo "Upgrading cluster '${clusterName}' to version: ${targetVersion} in region: ${region}"

    def upgradeResult = sh(
        script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl upgrade cluster --name=${clusterName} --version=${targetVersion} --region=${region} --approve",
        returnStdout: true
    ).trim()

    echo "Upgrade Result:\n${upgradeResult}"

    if (upgradeResult.contains("control plane has been upgraded to version")) {
        echo "✅ Cluster '${clusterName}' successfully upgraded to version ${targetVersion} in region ${region}."
    } else {
        error "❌ Cluster upgrade failed for '${clusterName}' to version ${targetVersion} in region ${region}."
    }
}
