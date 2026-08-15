#!/usr/bin/env bash
# Exports every service's OpenAPI spec into docs/api/ for the GitHub Pages site.
#
# Reads the specs from the running Kubernetes deployment (kubectl apply -k k8s/),
# port-forwarding each service in turn. Re-run this whenever an API changes.
set -euo pipefail

cd "$(dirname "$0")"
OUT=docs/api
NS=bankstack
mkdir -p "$OUT"

# service-name:container-port — must match k8s/services/*.yaml
services=(
  "auth-user:8094"
  "customer-service:8083"
  "account-service:8084"
  "biller-service:8088"
  "payment-orchestrator:8086"
  "billpay-worker:8090"
  # settlement-service is intentionally absent: it is a pure Kafka consumer with no
  # controllers, so it has no REST surface to document.
  "mcp-server:8095"
  "rag-service:8098"
  "multi-agent:8096"
)

# Forward to a high local port so we never collide with a service running locally
LOCAL_PORT=19000

for entry in "${services[@]}"; do
  svc=${entry%%:*}
  port=${entry##*:}

  kubectl -n "$NS" port-forward "svc/$svc" "$LOCAL_PORT:$port" >/dev/null 2>&1 &
  pf=$!
  # give the tunnel a moment, then pull the spec
  spec=""
  for _ in $(seq 1 10); do
    sleep 1
    spec=$(curl -s --max-time 5 "http://localhost:$LOCAL_PORT/v3/api-docs" || true)
    [ -n "$spec" ] && break
  done
  kill $pf 2>/dev/null || true
  wait $pf 2>/dev/null || true

  if [ -z "$spec" ]; then
    echo "!! $svc — no spec (is the pod running?)"
    continue
  fi

  printf '%s' "$spec" > "$OUT/$svc.json"

  # springdoc bakes the forwarded port into `servers`; rewrite it to the port the
  # service actually listens on so the published docs are not misleading.
  python3 -c '
import json, sys
svc, port, out = sys.argv[1], sys.argv[2], sys.argv[3]
with open(out) as f:
    spec = json.load(f)
spec["servers"] = [{
    "url": "http://localhost:" + port,
    "description": "Local — kubectl -n bankstack port-forward svc/%s %s:%s" % (svc, port, port),
}]
with open(out, "w") as f:
    json.dump(spec, f, indent=2)
print(len(spec.get("paths", {})))
' "$svc" "$port" "$OUT/$svc.json" | sed "s/^/   $svc — /;s/$/ paths/"
done

echo
echo "Specs written to $OUT/"
