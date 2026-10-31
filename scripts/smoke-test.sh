#!/usr/bin/env bash
# Smoke test against a RUNNING local LedgerFlow (default http://localhost:8080).
# Logs in as the seeded demo user alice and performs an idempotent transfer twice.
set -euo pipefail
BASE="${BASE_URL:-http://localhost:8080}"
JAR="$(mktemp)"; trap 'rm -f "$JAR"' EXIT
echo "== health =="; curl -fsS "$BASE/healthz"; echo
echo "== ready =="; curl -fsS "$BASE/readyz"; echo
echo "== login (alice) =="
TOKEN="$(curl -fsS -c "$JAR" "$BASE/login" | grep -o 'name="_csrf" value="[^"]*"' | head -1 | sed 's/.*value="//;s/"//')"
curl -fsS -b "$JAR" -c "$JAR" -o /dev/null -w "login POST -> %{http_code}\n" -d "username=alice&password=alice-demo-123&_csrf=$TOKEN" "$BASE/login"
echo "== accounts =="; ACCOUNTS="$(curl -fsS -b "$JAR" "$BASE/api/accounts")"; echo "$ACCOUNTS"
SRC="$(echo "$ACCOUNTS" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')"
# Destination: bob's account id is discovered in docs/verification.md for a fresh seed (alice=1, bob=2 on a clean DB).
DST="${DEST_ACCOUNT_ID:-2}"
echo "== transfer 2500 minor units, key smoke-1 (twice — must move money once) =="
curl -fsS -b "$JAR" -H 'Content-Type: application/json' -H 'Idempotency-Key: smoke-1' -d "{\"sourceAccountId\":$SRC,\"destinationAccountId\":$DST,\"amountMinor\":2500,\"currency\":\"USD\"}" "$BASE/api/transfers"; echo
curl -fsS -b "$JAR" -H 'Content-Type: application/json' -H 'Idempotency-Key: smoke-1' -d "{\"sourceAccountId\":$SRC,\"destinationAccountId\":$DST,\"amountMinor\":2500,\"currency\":\"USD\"}" "$BASE/api/transfers"; echo
echo "== accounts after =="; curl -fsS -b "$JAR" "$BASE/api/accounts"; echo
echo "SMOKE OK"
