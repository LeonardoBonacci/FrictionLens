#!/usr/bin/env bash
# install-operator.sh — Install the CloudNativePG operator into your cluster.
# Run once before applying the rest of the manifests.
#
# Usage: bash k8s/install-operator.sh

set -euo pipefail

CNPG_VERSION="1.25.0"

echo "Installing CloudNativePG operator v${CNPG_VERSION}..."
kubectl apply --server-side \
  -f "https://raw.githubusercontent.com/cloudnative-pg/cloudnative-pg/v${CNPG_VERSION}/releases/cnpg-${CNPG_VERSION}.yaml"

echo "Waiting for CNPG controller to be ready..."
kubectl rollout status deployment/cnpg-controller-manager \
  -n cnpg-system --timeout=120s

echo "CloudNativePG operator is ready."
