def call(String currentVersion, String targetVersion) {
    def parseVersion = { v ->
        v.tokenize('.').collect { it.toInteger() }
    }

    def current = parseVersion(currentVersion)
    def target = parseVersion(targetVersion)

    if (target <= current) {
        echo "✅ Allowed: Target version ${targetVersion} is <= current version ${currentVersion}"
        return true
    } else {
        error "❌ Denied: Target version ${targetVersion} is > current version ${currentVersion}"
    }
}

