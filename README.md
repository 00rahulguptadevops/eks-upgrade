# Upgrade cluster 

docker run --rm -v ~/.aws:/root/.aws -it public.ecr.aws/eksctl/eksctl:v0.207.0  upgrade cluster --name eks-cluster --region ap-south-1
