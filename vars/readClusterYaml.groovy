// vars/readClusterYaml.groovy

def call(String fileName) {
    if (!fileName || !fileExists(fileName)) {
        error "❌ File not uploaded or does not exist."
    }

    def yamlContent
    try {
        yamlContent = readYaml file: fileName
    } catch (e) {
        error "❌ YAML parsing failed: ${e.message}"
    }

    if (!(yamlContent instanceof List) || yamlContent.isEmpty()) {
        error "❌ Invalid or empty YAML."
    }

    return yamlContent[0]  // Return the first cluster object from the YAML
}

