def call(String clusterName, String nodegroupName, String region, String targeVersion) {
    echo "Upgrading nodegroup '${nodegroupName}' in cluster '${clusterName}'..."

    sh """
    /usr/local/bin/docker run --rm -v ~/.aws:/root/.aws public.ecr.aws/eksctl/eksctl upgrade nodegroup \
      --cluster ${clusterName} \
      --name ${nodegroupName} \
      --region ${region} \
      --kubernetes-version ${targeVersion}
    """

    echo "✅ Upgrade complete for nodegroup '${nodegroupName}'"
}
