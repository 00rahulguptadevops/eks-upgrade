output=$(docker run -it --rm \
  -v "$HOME/.kube/config:/.kubeconfig" \
  --network host kubent:01 \
  -t 1.32 -o json -e -k /.kubeconfig 2>/dev/null | awk '/^\[/{f=1} f' )

# Check for deprecated APIs
if echo "$output" | jq -e 'length > 0' >/dev/null; then
  echo "❌ Deprecated APIs found! Please fix the following:"
  echo "$output" | jq -r '.[] | "- \(.Kind) (\(.ApiVersion)) in namespace \(.Namespace), replace with: \(.ReplaceWith)"'
  exit 1
else
  echo "✅ No deprecated APIs detected."
  exit 0
fi

