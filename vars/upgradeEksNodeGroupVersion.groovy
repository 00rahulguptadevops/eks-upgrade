def call(Map clusterInfo) {
    def nodegroupVersionMap = checkEksNodeGroupVersion(clusterInfo.name, clusterInfo.region)

    def skipped = []
    def upgraded = []
    def failures = []
    def nodegroupsToUpgrade = []

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

    input(
        id: 'NodegroupUpgradeApproval',
        message: "🚀 Proceed with upgrading the following nodegroups?\n${nodegroupsToUpgrade.join(', ')}",
        ok: 'Yes'
    )

    nodegroupsToUpgrade.each { nodepool ->
        slackNotifier.notifyStage("Nodegroup Upgrade: ${nodepool}", clusterInfo.slack_channel) {
            echo "🔧 Starting upgrade for nodepool '${nodepool}'..."
            def result = upgradeEksNodeGroupVersion(
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

