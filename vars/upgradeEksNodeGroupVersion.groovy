def call(Map clusterInfo) {
    echo "ℹ️ Starting EKS nodegroup check/upgrade for cluster: ${clusterInfo.name}"

    // Step 1: Fetch current nodegroup versions
    def nodegroupVersionMap = checkEksNodeGroupVersion(clusterInfo.name, clusterInfo.region)

    def skipped = []
    def upgraded = []
    def failures = []
    def nodegroupsToUpgrade = []

    // Step 2: Determine which nodegroups require upgrade
    clusterInfo.node_pools.each { nodepool ->
        def currentVersion = nodegroupVersionMap[nodepool]
        if (!currentVersion) {
            echo "⚠️ Warning: Nodegroup '${nodepool}' not found in cluster."
            failures << nodepool
        } else if (currentVersion != clusterInfo.target_version) {
            echo "🔧 Nodegroup '${nodepool}' version is ${currentVersion}. Target is ${clusterInfo.target_version}. Upgrade required."
            nodegroupsToUpgrade << nodepool
        } else {
            echo "✅ Nodegroup '${nodepool}' is already at target version ${currentVersion}. Skipping upgrade."
            skipped << nodepool
        }
    }

    if (nodegroupsToUpgrade.isEmpty()) {
        echo "🎉 All nodegroups are already up-to-date."
        return
    }

    // Step 3: Prompt user for confirmation
    input(
        id: 'NodegroupUpgradeApproval',
        message: "🚀 Proceed with upgrading the following nodegroups?\n${nodegroupsToUpgrade.join(', ')}",
        ok: 'Yes'
    )

    // Step 4: Perform upgrades
    nodegroupsToUpgrade.each { nodepool ->
        slackNotifier.notifyStage("Nodegroup Upgrade: ${nodepool}", clusterInfo.slack_channel) {
            echo "🔧 Starting upgrade for nodepool '${nodepool}'..."

            def result = upgradeNodegroup(
                clusterInfo.name,
                nodepool,
                clusterInfo.region,
                clusterInfo.target_version
            )

            if (result == "true") {
                upgraded << nodepool
                return ":white_check_mark: Upgrade complete for nodegroup '${nodepool}'"
            } else {
                failures << nodepool
                return ":x: Upgrade failed for nodegroup '${nodepool}'"
            }
        }
    }

    echo "✅ Upgraded: ${upgraded}"
    echo "🟦 Skipped: ${skipped}"
    echo "❌ Failures: ${failures}"

    if (failures) {
        error("❌ Some nodegroups failed to upgrade: ${failures.join(', ')}")
    }
}

// Internal helper: Check nodegroup versions
def checkEksNodeGroupVersion(String clusterName, String region) {
    def nodegroupJson = sh(
        script: "/usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl get nodegroup --cluster ${clusterName} --region ${region} -o json",
        returnStdout: true
    )
    def nodegroups = readJSON text: nodegroupJson
    def result = [:]
    nodegroups.each { ng ->
        result[ng.Name] = ng.Version
        echo "🔍 Current version of '${ng.Name}': ${ng.Version}"
    }
    return result
}

// Internal helper: Upgrade a nodegroup
def upgradeNodegroup(String cluster, String nodegroup, String region, String version) {
    echo "⬆️ Upgrading '${nodegroup}' in cluster '${cluster}' to version '${version}' in region '${region}'"
    try {
        sh """
            /usr/local/bin/docker run --rm \
                -v ~/.aws:/root/.aws \
                public.ecr.aws/eksctl/eksctl \
                upgrade nodegroup \
                --name ${nodegroup} \
                --cluster ${cluster} \
                --region ${region} \
                --kubernetes-version ${version} \
        """
        return "true"
    } catch (Exception e) {
        echo "❌ Upgrade failed: ${e.getMessage()}"
        return "false"
    }
}
