def call(List addons, String clusterVersion, String clusterName, String region) {
    def toUpgrade = []
    def failedToValidate = []
    def summary = ""

    addons.each { addon ->
        def name = addon.name
        def targetVersion = addon.version

        echo "🔍 Validating add-on: ${name}"

        def currentVersion = sh(
            script: """
                /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws amazon/aws-cli eks describe-addon \
                    --region ${region} \
                    --cluster-name ${clusterName} \
                    --addon-name ${name} \
                    --query 'addon.addonVersion' \
                    --output text
            """,
            returnStdout: true
        ).trim()

        if (currentVersion == targetVersion) {
            summary += ":arrow_right: ${name} already up to date (${targetVersion})\n"
            return
        }

        def validVersions = sh(
            script: """
                /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws amazon/aws-cli eks describe-addon-versions \
                    --region ${region} \
                    --addon-name ${name} \
                    --kubernetes-version ${clusterVersion} \
                    --query 'addons[0].addonVersions[*].addonVersion' \
                    --output text
            """,
            returnStdout: true
        ).trim().tokenize()

        if (!validVersions.contains(targetVersion)) {
            summary += ":x: ${targetVersion} is NOT compatible with ${name} on EKS ${clusterVersion}\n"
            failedToValidate << addon
            return
        }

        summary += ":gear: ${name} will be upgraded from ${currentVersion} → ${targetVersion}\n"
        toUpgrade << addon
    }

    // ❌ Abort pipeline if validation failed
    if (!failedToValidate.isEmpty()) {
        def failedNames = failedToValidate.collect { it.name }.join(', ')
        summary += "\n❌ Validation failed for: ${failedNames}"
        echo summary
        return [
            summary: summary,
            failedAddons: failedToValidate
        ]
    }

    // ✅ All add-ons validated
    if (toUpgrade.isEmpty()) {
        summary += "✅ All add-ons are already up to date\n"
        return [
            summary: summary,
            failedAddons: []
        ]
    }

    // 🛑 Ask for approval before upgrading
    input message: "Proceed with upgrading ${toUpgrade.size()} add-ons?", ok: "Yes, upgrade"

    toUpgrade.each { addon ->
        def name = addon.name
        def version = addon.version

        try {
            echo "🚀 Upgrading ${name} to ${version}"
            sh """
                /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl update addon \
                    --name '${name}' \
                    --cluster '${clusterName}' \
                    --version '${version}' \
                    --region '${region}' \
                    --force
            """
            summary += ":white_check_mark: ${name} upgraded to ${version}\n"
        } catch (err) {
            summary += ":x: ${name} failed to upgrade - ${err.getMessage()}\n"
        }
    }

    return [
        summary: summary,
        failedAddons: []
    ]
}
