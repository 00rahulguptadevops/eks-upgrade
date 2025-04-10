#!/bin/bash

CLUSTER_NAME="eksclt-cluster-v1"
REGION="eu-north-1"
K8S_VERSION="1.31"
EKSCTL_IMAGE="public.ecr.aws/eksctl/eksctl:v0.207.0"

# Get list of nodegroups as clean JSON
NODEGROUPS=$(docker run --rm -i \
  --platform linux/amd64 \
  -v ~/.aws:/root/.aws \
  -v ~/.kube:/root/.kube \
  $EKSCTL_IMAGE \
  get nodegroup \
  --cluster "$CLUSTER_NAME" \
  --region "$REGION" \
  --output json \
  --color never | jq -r '.[].Name')

if [ -z "$NODEGROUPS" ]; then
  echo "❌ No nodegroups found or failed to parse nodegroups. Exiting."
  exit 1
fi

# Loop and upgrade each nodegroup
for NG in $NODEGROUPS; do
  echo "🔄 Upgrading nodegroup: $NG"
  docker run --rm -i \
    --platform linux/amd64 \
    -v ~/.aws:/root/.aws \
    -v ~/.kube:/root/.kube \
    $EKSCTL_IMAGE \
    upgrade nodegroup \
    --cluster "$CLUSTER_NAME" \
    --name "$NG" \
    --region "$REGION" \
    --kubernetes-version "$K8S_VERSION" 
done

