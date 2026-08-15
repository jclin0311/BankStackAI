#!/usr/bin/env bash
# Fetches an Auth0 access token (client_credentials) to paste into Swagger UI's
# "Authorize" box, or to use directly:
#
#   ./get-token.sh                       # print the token
#   TOKEN=$(./get-token.sh)              # use it in curl
#   curl -H "Authorization: Bearer $TOKEN" http://localhost:8084/api/v1/accounts
#
# Credentials come from .env (loaded by direnv); run from the repo root.
set -euo pipefail

: "${AUTH0_M2M_CLIENT_ID:?not set — is direnv loading .env? try: direnv allow}"
: "${AUTH0_M2M_CLIENT_SECRET:?not set — is direnv loading .env? try: direnv allow}"

ISSUER="${AUTH0_ISSUER_URI:-https://dev-4mxqf6pdu0czpsbp.us.auth0.com/}"
AUDIENCE="${AUTH0_AUDIENCE:-https://mockbank/api}"

response=$(curl -s --request POST "${ISSUER}oauth/token" \
  --header 'content-type: application/json' \
  --data @- <<EOF
{
  "client_id": "${AUTH0_M2M_CLIENT_ID}",
  "client_secret": "${AUTH0_M2M_CLIENT_SECRET}",
  "audience": "${AUDIENCE}",
  "grant_type": "client_credentials"
}
EOF
)

python3 - "$response" <<'PY'
import json, sys
data = json.loads(sys.argv[1])
if "access_token" not in data:
    print(f"Auth0 error: {data.get('error')}: {data.get('error_description')}", file=sys.stderr)
    sys.exit(1)
print(data["access_token"])
PY
