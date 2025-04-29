def call(List addons, String clusterVersion) {
    def toUpgrade = []

    addons.each { addon ->
        def name = addon.name
        def targetVersion = addon.version

        // Get current version using eksctl
        def currentVersion = sh(
            script: "eksctl get addon --name ${name} --cluster ${env.CLUSTER_NAME} -o json | jq -r '.[0].AddonVersion'",
            returnStdout: true
        ).trim()

        if (currentVersion == targetVersion) {
            echo "✅ ${name} is already at version ${targetVersion}"
        } else {
            // Validate target version is available
            def availableVersions = sh(
                script: "eksctl describe addon-versions --name ${name} --kubernetes-version ${clusterVersion} -o json | jq -r '.[0].AddonVersions[].AddonVersion'",
                returnStdout: true
            ).readLines().collect { it.trim() }

            if (!availableVersions.contains(targetVersion)) {
                error "❌ Version ${targetVersion} is NOT valid for ${name} on EKS ${clusterVersion}"
            }

            toUpgrade << addon
        }
    }

    if (toUpgrade.isEmpty()) {
        echo "✅ All add-ons are already up to date."
        return
    }

    echo "🔧 Add-ons to upgrade:"
    toUpgrade.each { echo "- ${it.name} → ${it.version}" }

    input message: "Proceed with upgrading ${toUpgrade.size()} add-ons?", ok: "Yes"

    toUpgrade.each { addon ->
        echo "🚀 Upgrading ${addon.name} to ${addon.version}"
        sh """
            eksctl update addon \
              --name ${addon.name} \
              --cluster ${env.CLUSTER_NAME} \
              --version ${addon.version} \
              --force
        """
        echo "✅ ${addon.name} upgraded"
    }

    echo "🎉 Upgrade process completed."
}
