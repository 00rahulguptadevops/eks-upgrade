def call(String currentVersion, String targetVersion) {
    def parseVersion = { v ->
        def parts = v.tokenize('.').collect { it.toInteger() }
        return [major: parts[0], minor: parts[1]]
    }

    def current = parseVersion(currentVersion)
    def target = parseVersion(targetVersion)

    if (target.major < current.major || 
        (target.major == current.major && target.minor <= current.minor)) {
        echo "✅ Allowed: Target version ${targetVersion} is <= current version ${currentVersion}"
        return true
    } else {
        error "❌ Denied: Target version ${targetVersion} is > current version ${currentVersion}"
    }
}

