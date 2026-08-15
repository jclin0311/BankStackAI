#!/usr/bin/env bash
# Creates/updates the bankstack-auth0 secret from the repo's .env file.
# Run from the repo root: ./k8s/create-auth0-secret.sh
set -euo pipefail

ENV_FILE="$(dirname "$0")/../.env"
if [ ! -f "$ENV_FILE" ]; then
  echo "No .env file found at $ENV_FILE" >&2
  exit 1
fi

kubectl create secret generic bankstack-auth0 \
  --namespace bankstack \
  --from-env-file="$ENV_FILE" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "bankstack-auth0 secret updated."
