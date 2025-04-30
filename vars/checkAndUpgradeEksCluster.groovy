def call(String clusterName, String region, String targetVersion) {
    try {
        echo "Checking current version of cluster '${clusterName}' in region '${region}'..."

        def currentVersion = sh(
            script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl get cluster --name ${clusterName} --region ${region} -o json | jq -r '.[0].Version'",
            returnStdout: true
        ).trim()

        echo "Current version of the cluster: ${currentVersion}"

        if (currentVersion == targetVersion) {
            def msg = "ℹ️ Cluster '${clusterName}' is already at version ${targetVersion} in region ${region}. Skipping upgrade."
            echo msg
            return [
                summary: msg,
                status: "skipped"
            ]
        }

        try {
            timeout(time: 5, unit: 'MINUTES') {
                input message: "Approve upgrade of cluster '${clusterName}' from ${currentVersion} to ${targetVersion}?"
            }
        } catch (err) {
            def msg = "❌ Upgrade was cancelled or timed out for cluster '${clusterName}' in region ${region}."
            echo msg
            return [
                summary: msg,
                status: "cancelled"
            ]
        }

        echo "Upgrading cluster '${clusterName}' to version: ${targetVersion} in region: ${region}"

        def upgradeResult = sh(
            script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl upgrade cluster --name=${clusterName} --version=${targetVersion} --region=${region} --approve",
            returnStdout: true
        ).trim()

        echo "Upgrade Result:\n${upgradeResult}"

        if (upgradeResult.contains("control plane has been upgraded to version")) {
            def msg = "✅ Cluster '${clusterName}' successfully upgraded to version ${targetVersion} in region ${region}."
            echo msg
            return [
                summary: msg,
                status: "success"
            ]
        } else {
            def msg = "❌ Upgrade failed for cluster '${clusterName}' to version ${targetVersion} in region ${region}."
            echo msg
            return [
                summary: msg,
                status: "failure"
            ]
        }
    } catch (err) {
        def msg = "❌ Unexpected error occurred while processing cluster '${clusterName}': ${err.message}"
        echo msg
        return [
            summary: msg,
            status: "failure"
        ]
    }
}
