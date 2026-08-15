#!/usr/bin/env bash
# Builds a Docker image per service from the jars already produced by build-all.sh.
# Usage: ./build-images.sh [tag]   (default tag: local)
set -euo pipefail

TAG="${1:-local}"

# module-dir:image-name pairs (image names match k8s/services/*.yaml)
services=(
  "AuthUser:auth-user"
  "CusomerService:customer-service"
  "AccountService:account-service"
  "BillerService:biller-service"
  "PaymentOrchestrator:payment-orchestrator"
  "BillPayWorkerService:billpay-worker"
  "SettlementService:settlement-service"
  "BankStackMCPServer:mcp-server"
  "BankStackRag:rag-service"
  "BankStackMultiAgent:multi-agent"
)

for entry in "${services[@]}"; do
  module=${entry%%:*}
  image=${entry##*:}
  echo "==> bankstack/${image}:${TAG} (from ${module})"
  docker build -q -t "bankstack/${image}:${TAG}" "${module}"
done

echo "All images built."
