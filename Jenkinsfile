pipeline {
    agent any
    environment {
        K8S_TARGET_VERSION = '1.32'  // Set the Kubernetes API version as an environment variable
    }
    parameters {
        base64File(name: 'KUBECONFIG', description: 'Upload your kubeconfig file')
    }
    stages {
        stage('Check for Deprecated APIs') {
            steps {
                withFileParameter('KUBECONFIG') {
                    script {
                        def output = sh(
                            script: '''
                                /usr/local/bin/docker run -i --rm \
                                -v "$KUBECONFIG:/root/.kube/config" \
                                --network host cathit/kubent:latest \
                                -t $K8S_TARGET_VERSION -o json -e -k /root/.kube/config 2>/dev/null
                            ''',
                            returnStdout: true
                        ).trim()
                        
                        if (output) {
                            def jsonOutput = readJSON text: output
                            if (jsonOutput) {
                                echo "❌ Deprecated APIs found! Please address the following:"
                                jsonOutput.each { item ->
                                    echo "- ${item.kind} (${item.apiversion}) in namespace ${item.namespace}, replace with: ${item.replacement}"
                                }
                                error("Deprecated APIs detected.")
                            } else {
                                echo "✅ No deprecated APIs detected."
                            }
                        } else {
                            echo "✅ No deprecated APIs detected."
                        }
                    }
                }
            }
        }
    }
}
