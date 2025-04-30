def call(String clusterName, String region, String targetVersion) {
    echo "Upgrading cluster '${clusterName}' to version: ${targetVersion} in region: ${region}"

    def upgradeResult = sh(
        script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl upgrade cluster --name=${clusterName} --version=${targetVersion} --region=${region} --approve",
        returnStdout: true
    ).trim()

    echo "Upgrade Result:\n${upgradeResult}"

    if (upgradeResult.contains("control plane has been upgraded to version")) {
        return [
            summary: "✅ Cluster '${clusterName}' successfully upgraded to version ${targetVersion} in region ${region}.",
            status: "success"
        ]
    } else {
        echo "Cluster upgrade failed for '${clusterName}'"
        return [
            summary: "❌ Upgrade failed for cluster '${clusterName}' to version ${targetVersion} in region ${region}.",
            status: "failure"
        ]
    }
}
