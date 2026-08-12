#!/usr/bin/env bash

set -euo pipefail

# Build order matters:
# 1. shared libraries first (services depend on them via mvn install)
# 2. core banking services
# 3. AI / agent services
modules=(
  "commons-dto"
  "commons-security"
  "commons-observability"
  "CusomerService"
  "AccountService"
  "PaymentOrchestrator"
  "BillerService"
  "BillPayWorkerService"
  "SettlementService"
  "AuthUser"
  "BankStackMCPServer"
  "BankStackRag"
  "BankStackMultiAgent"
  "BankStackEval"
)

for module in "${modules[@]}"; do
  echo
  echo "=================================================="
  echo "Building ${module}"
  echo "=================================================="

  (
    cd "${module}"
    mvn clean install -DskipTests
  )
done

echo
echo "BankStackAI build completed successfully."
