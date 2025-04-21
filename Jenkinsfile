pipeline {
    agent any
    environment {
        K8S_TARGET_VERSION = '1.32'
        DOCKER_IMAGE = 'kubent:aws01'
        EKS_CLUSTER_NAME = 'eks-cluster-v1'
        AWS_REGION = 'ap-south-1'
        EKSCTL_IMAGE = 'public.ecr.aws/eksctl/eksctl:v0.207.0'
        EKS_ADDONS = 'vpc-cni,coredns,kube-proxy' // comma-separated addon names
        EKS_ADDONS_VERSION = 'v1.19.2-eksbuild.1,v1.11.4-eksbuild.2,v1.31.3-eksbuild.2' // corresponding versions
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
                            script: """
                                /usr/local/bin/docker run -i --rm \
                                -v "$KUBECONFIG:/root/.kube/config" \
                                -v ~/.aws:/root/.aws \
                                ${DOCKER_IMAGE} \
                                -t ${K8S_TARGET_VERSION} -o json -e -k /root/.kube/config 2>/dev/null
                            """,
                            returnStdout: true
                        ).trim()

                        if (output) {
                            def jsonOutput = readJSON text: output
                            if (jsonOutput) {
                                echo "❌ Deprecated APIs found:"
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

        stage('Validate EKS Cluster') {
            steps {
                script {
                    def upgradeResult = sh(
                        script: """
                            /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws \
                            ${EKSCTL_IMAGE} upgrade cluster \
                            --name ${EKS_CLUSTER_NAME} \
                            --region ${AWS_REGION} \
                            --version ${K8S_TARGET_VERSION}
                        """,
                        returnStdout: true
                    ).trim()

                    if (upgradeResult.contains("Error: cannot upgrade to a lower version")) {
                        echo "❌ Cluster validation failed: ${upgradeResult}"
                        currentBuild.result = 'FAILURE'
                        error("Stopping pipeline.")
                    } else {
                        echo "✅ Cluster validated successfully."
                        echo upgradeResult
                    }
                }
            }
        }

        stage('Upgrade EKS Cluster') {
            steps {
                input message: 'Do you want to upgrade the EKS cluster to the target version?', ok: 'Yes, Upgrade'
                script {
                    def upgradeStatus = sh(
                        script: """
                            /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws \
                            ${EKSCTL_IMAGE} upgrade cluster \
                            --name ${EKS_CLUSTER_NAME} \
                            --region ${AWS_REGION} \
                            --version ${K8S_TARGET_VERSION} \
                            --approve
                        """,
                        returnStdout: true
                    ).trim()

                    echo "✅ EKS cluster upgrade started to version ${K8S_TARGET_VERSION}."
                    echo upgradeStatus
                }
            }
        }

        stage('Upgrade All EKS Nodegroups') {
            steps {
                script {
                    def nodegroupsText = sh(
                        script: """
                            /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws \
                            ${EKSCTL_IMAGE} get nodegroup \
                            --cluster ${EKS_CLUSTER_NAME} \
                            --region ${AWS_REGION} \
                            -o json
                        """,
                        returnStdout: true
                    ).trim()

                    def nodegroupsJson = readJSON text: nodegroupsText
                    def nodegroups = nodegroupsJson.collect { it.Name }

                    if (nodegroups.size() == 0) {
                        echo "⚠️ No nodegroups found in the cluster."
                        return
                    }

                    nodegroups.each { nodegroup ->
                        echo "⬆️ Upgrading nodegroup: ${nodegroup}"

                        def upgradeOutput = sh(
                            script: """
                                /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws \
                                ${EKSCTL_IMAGE} upgrade nodegroup \
                                --name ${nodegroup} \
                                --cluster ${EKS_CLUSTER_NAME} \
                                --region ${AWS_REGION} \
                                --kubernetes-version ${K8S_TARGET_VERSION} \
                                --wait=false
                            """,
                            returnStdout: true
                        ).trim()

                        echo "✅ Upgrade output for ${nodegroup}:\n${upgradeOutput}"
                    }
                }
            }
        }


                stage('Upgrade EKS Addons') {
            steps {
                script {
                    def addonNames = EKS_ADDONS.split(',')
                    def addonVersions = EKS_ADDONS_VERSION.split(',')

                    if (addonNames.size() != addonVersions.size()) {
                        error("Mismatch in number of addon names and versions.")
                    }

                    addonNames.eachWithIndex { addon, index ->
                        def version = addonVersions[index]
                        echo "⬆️ Upgrading addon: ${addon} to version: ${version}"

                        def addonOutput = sh(
                            script: """
                                /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws \
                                ${EKSCTL_IMAGE} update addon \
                                --name ${addon} \
                                --cluster ${EKS_CLUSTER_NAME} \
                                --region ${AWS_REGION} \
                                --version ${version} \
                                --force
                            """,
                            returnStdout: true
                        ).trim()

                        echo "✅ Upgrade output for addon ${addon}:\n${addonOutput}"
                    }
                }
            }
        }

    }

}
