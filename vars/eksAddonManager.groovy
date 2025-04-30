def call(List addons, String clusterVersion, String clusterName, String region) {
    def toUpgrade = []
    def results = []
    def summary = ""

    addons.each { addon ->
        def name = addon.name
        def targetVersion = addon.version

        echo "🔍 Getting current version of ${name}"

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
            echo "✅ ${name} already at version ${targetVersion}"
            results << [name: name, version: targetVersion, status: 'skipped']
            summary += ":arrow_right: ${name} already up to date (${targetVersion})\n"
            return
        }

        echo "🔍 Validating compatibility of ${targetVersion} for ${name} on EKS ${clusterVersion}"

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
            error "❌ ${targetVersion} is NOT compatible with ${name} on EKS ${clusterVersion}"
        }

        toUpgrade << addon
    }

    if (toUpgrade.isEmpty()) {
        echo "✅ All add-ons are already up to date"
        return [results: results, summary: summary]
    }

    echo "🛠️ Add-ons to upgrade:"
    toUpgrade.each { echo "- ${it.name} → ${it.version}" }

    input message: "Do you want to proceed with upgrading ${toUpgrade.size()} add-ons?", ok: "Approve"

    toUpgrade.each { addon ->
        def name = addon.name
        def version = addon.version
        try {
            echo "🚀 Upgrading ${name} to version ${version}"
            sh """
                /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl update addon \
                    --name ${name} \
                    --cluster ${clusterName} \
                    --version ${version} \
                    --region ${region} \
                    --force
            """
            echo "✅ ${name} upgraded to ${version}"
            results << [name: name, version: version, status: 'updated']
            summary += ":white_check_mark: ${name} upgraded to ${version}\n"
        } catch (err) {
            echo "❌ Failed to upgrade ${name}: ${err.getMessage()}"
            results << [name: name, version: version, status: 'failed']
            summary += ":x: ${name} failed to upgrade\n"
        }
    }

    return [results: results, summary: summary]
}
