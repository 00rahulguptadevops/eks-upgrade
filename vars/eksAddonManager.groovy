def call(List addons, String clusterVersion, String clusterName, String region) {
    def toUpgrade = []
    def failedToValidate = []
    def results = []
    def preSummary = ""
    def postSummary = ""

    addons.each { addon ->
        def name = addon.name
        def targetVersion = addon.version

        echo "🔍 Checking ${name}"

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
            preSummary += ":arrow_right: ${name} already up to date (${targetVersion})\n"
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
            def msg = ":x: ${targetVersion} is NOT compatible with ${name} on EKS ${clusterVersion}"
            echo msg
            results << [name: name, version: targetVersion, status: 'invalid']
            failedToValidate << [name: name, version: targetVersion]
            preSummary += "${msg}\n"
            return
        }

        toUpgrade << addon
        preSummary += ":gear: ${name} will be upgraded from ${currentVersion} → ${targetVersion}\n"
    }

    if (toUpgrade.isEmpty() && failedToValidate.isEmpty()) {
        preSummary += "\n✅ All add-ons are already up to date"
        return [summary: preSummary, failedToValidate: failedToValidate]
    }

    input message: "Proceed with upgrading ${toUpgrade.size()} add-ons?", ok: "Yes, upgrade"

    toUpgrade.each { addon ->
        def name = addon.name
        def version = addon.version

        try {
            echo "🚀 Upgrading ${name} to ${version}"
            sh """
                set -euo pipefail
                /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl update addon \
                    --name '${name}' \
                    --cluster '${clusterName}' \
                    --version '${version}' \
                    --region '${region}' \
                    --force
            """
            echo "✅ ${name} upgraded to ${version}"
            results << [name: name, version: version, status: 'updated']
            postSummary += ":white_check_mark: ${name} upgraded to ${version}\n"
        } catch (err) {
            echo "❌ Failed to upgrade ${name}: ${err.getMessage()}"
            results << [name: name, version: version, status: 'failed']
            postSummary += ":x: ${name} failed to upgrade\n"
        }
    }

    return [
        summary: preSummary + "\n---\n" + postSummary,
        failedToValidate: failedToValidate
    ]
}
