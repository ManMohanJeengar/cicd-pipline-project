#!/usr/bin/env bash
# "Monitor" step of the canary release.
# Sends traffic through the gateway, then checks
#   1. no request failed (error rate must be 0)
#   2. about EXPECTED_PCT % of the traffic was served by the canary
# Exit code 0 = canary is healthy (promote), 1 = canary is bad (roll back).
set -euo pipefail

URL="${1:-http://localhost:8080}"
REQUESTS="${REQUESTS:-100}"
EXPECTED_PCT="${EXPECTED_PCT:-10}"
TOLERANCE="${TOLERANCE:-5}"

canary=0
stable=0
errors=0
body_file="$(mktemp)"

echo "Sending ${REQUESTS} requests to ${URL} ..."
for _ in $(seq 1 "$REQUESTS"); do
  code="$(curl -s -o "$body_file" -w '%{http_code}' "${URL}/info" || echo 000)"
  if [ "$code" != "200" ]; then
    errors=$((errors + 1))
    continue
  fi
  if grep -q '"track":"canary"' "$body_file"; then
    canary=$((canary + 1))
  else
    stable=$((stable + 1))
  fi
done

# also exercise the real API through the gateway
api_code="$(curl -s -o /dev/null -w '%{http_code}' "${URL}/posts" || echo 000)"
[ "$api_code" = "200" ] || errors=$((errors + 1))

pct=$((canary * 100 / REQUESTS))
echo "stable=${stable} canary=${canary} errors=${errors} canary_share=${pct}% (expected ~${EXPECTED_PCT}%)"

if [ "$errors" -gt 0 ]; then
  echo "FAIL: ${errors} failed request(s) - roll back the canary"
  exit 1
fi

diff=$((pct - EXPECTED_PCT))
[ "$diff" -lt 0 ] && diff=$((-diff))
if [ "$diff" -gt "$TOLERANCE" ]; then
  echo "FAIL: canary traffic share is outside ${EXPECTED_PCT}% +/- ${TOLERANCE}%"
  exit 1
fi

echo "OK: canary is healthy - safe to promote"
