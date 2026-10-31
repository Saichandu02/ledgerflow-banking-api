# Demo

Local URL (when running): http://localhost:8080 — sign in as `alice` / `alice-demo-123`.

## 60–90 second recording script (recording itself: PENDING — no screen capture was made in the build sandbox)
1. (0:00) Open http://localhost:8080, sign in as alice. Show Alice Checking: 100000 minor units ($1,000.00).
2. (0:15) Via the smoke script / curl, POST a 2500-minor-unit transfer to account 2 with `Idempotency-Key: demo-1`. Show 201 + transfer id.
3. (0:35) Send the **identical** request again. Show the same transfer id, and alice's balance is 97500 — money moved once.
4. (0:55) Open alice's statement: one DEBIT −2500 row. Explain the matching +2500 CREDIT on bob's account (double-entry, sums to zero).
5. (1:15) Try a transfer larger than the balance. Show HTTP 422 insufficient funds and an unchanged balance.

## Scripted version
`scripts/smoke-test.sh` performs steps 2–3 over HTTP against a running instance and prints both responses and the before/after account lists.
