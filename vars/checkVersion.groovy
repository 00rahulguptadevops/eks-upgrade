def call(String currentVersion, String targetVersion) {
    def parseVersion = { v ->
        def parts = v.tokenize('.').collect { it.toInteger() }
        return [major: parts[0], minor: parts[1]]
    }

    def current = parseVersion(currentVersion)
    def target = parseVersion(targetVersion)

    if (target.major == current.major && target.minor == current.minor) {
        echo "✅ Your cluster is already running with version ${targetVersion}"
        return true
    } else if (target.major == current.major && target.minor == current.minor + 1) {
        echo "✅ Allowed: Upgrade to version ${targetVersion} is permitted from ${currentVersion}"
        return true
    } else {
        error "❌ Denied: Target version ${targetVersion} is not allowed from current version ${currentVersion}"
    }
}

