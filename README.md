# LedgerFlow — Transaction-Safe Banking Sandbox

A small banking simulator (fictional accounts only) whose one memorable feature is a **transfer that stays correct under retries and simultaneous requests**: send the same transfer twice and money moves once; send concurrent transfers that exceed the balance and the account never goes negative.

Personal project by Sai Chandu Machavarapu, demonstrating Java, Spring Boot, Spring Data JPA/Hibernate, SQL, REST APIs, and JUnit. Supporting additions used here: Flyway, Thymeleaf, Testcontainers (CI/Docker environments).

> Screenshot: **pending** — no screenshot was genuinely captured in the build environment; see `docs/demo.md` for the recording script. Public demo: **not deployed** (see Deployment). Local URL when running: http://localhost:8080

## Implemented capabilities
1. **Double-entry ledger in integer minor units** — every transfer writes a −amount and +amount entry summing to zero; cached balances are reconciled against the ledger in tests. No floating-point money.
2. **Idempotency-Key contract enforced in the database** — same key + same payload returns the original transfer; same key + different payload returns HTTP 409.
3. **Concurrency-safe transfers** — both accounts locked `FOR UPDATE` in ascending ID order inside one transaction; insufficient funds (422), same-account, invalid amount, currency mismatch, and other-user debits (403) are all rejected with nothing moving.

## Quickstart

Prerequisites: Java 21, Maven (or use the wrapper after `./mvnw` is generated), PostgreSQL 16 — **or** just Docker.

### Docker (recommended on a machine with Docker)
```bash
docker compose up --build
# open http://localhost:8080 — sign in: alice / alice-demo-123
```

### Local PostgreSQL
```bash
createuser -s ledgerflow 2>/dev/null || true   # or your distro's equivalent
psql -c "ALTER USER ledgerflow PASSWORD 'ledgerflow-local-only';"
createdb -O ledgerflow ledgerflow
mvn spring-boot:run
# open http://localhost:8080 — sign in: alice / alice-demo-123  (bob / bob-demo-123)
```
On Linux: `sudo -u postgres psql …`. On Windows (PowerShell), the same `mvn spring-boot:run` works once PostgreSQL is installed and the database/user above exist.

Seed data (local only): alice with account funded 100000 minor units ($1,000.00) and bob with 50000 ($500.00). On a clean DB alice's account is id 1 and bob's is id 2.

### Smoke test
```bash
bash scripts/smoke-test.sh   # logs in as alice, sends the same 2500 transfer twice, prints balances
```

## API

| Endpoint | Description |
|---|---|
| `GET /healthz` | Liveness (no DB check) |
| `GET /readyz` | Readiness (503 if the DB connection fails) |
| `GET /api/accounts` | The signed-in user's accounts |
| `GET /api/accounts/{id}/statement?page=0` | Ledger entries for an owned account (403 otherwise) |
| `POST /api/transfers` | Transfer; requires `Idempotency-Key` header |

```bash
curl -b cookies.txt -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d '{"sourceAccountId":1,"destinationAccountId":2,"amountMinor":2500,"currency":"USD"}' \
  http://localhost:8080/api/transfers
# 201 {"id":1,"status":"COMMITTED","amountMinor":2500,"currency":"USD","sourceAccountId":1,"destinationAccountId":2}
# Repeating the identical request returns the same transfer id and moves no additional money.
```
OpenAPI UI: http://localhost:8080/swagger-ui.html

## Tests
```bash
# integration tests need a real PostgreSQL test database:
createdb -O ledgerflow ledgerflow_test
mvn verify
```
Suite: transfer + balanced-ledger + reconciliation, idempotent retry, key/payload conflict (409), insufficient funds (422), unauthorized debit (403), same-account/invalid amount, **10-thread concurrency run (10×20000 against a 100000 balance — exactly 5 succeed, balance ends at 0)**, plus MockMvc auth/CSRF-surface tests. Actual results in this build environment: `docs/verification.md`.

## Architecture
See `docs/architecture.md` (diagram + money model), `docs/decisions.md` (locking, idempotency, CSRF tradeoffs), `docs/demo.md`, `docs/verification.md`.

## Deployment
Local Docker Compose is the primary route. Single-VM public route (not executed — no deployment account was authorized): provision a small VM, run `docker compose up -d` behind a Caddy/nginx HTTPS reverse proxy, persist the `ledgerflow-db` volume, back up with `pg_dump` / restore with `psql`, let Flyway run migrations at startup, **change the DB password and remove the demo seeder first**. Resource needs: ~1 vCPU / 1 GB RAM for app + PostgreSQL at demo scale.

## Limitations & tradeoffs
- Single currency (USD); no FX, fees, or scheduled payments.
- Pessimistic locking serializes transfers touching the same account — correctness first, throughput second.
- Demo seed users have published passwords — local use only; there is no public-demo write rate limiting yet.
- Session-cookie API auth with CSRF excluded for `/api/**` (documented in `docs/decisions.md`); a production API should use token auth.
- This is a simulator: not banking-grade, not connected to any real bank, no real money.

## Resume bullets (personal project — draft, based only on what is demonstrated here)
- Built a transaction-safe banking sandbox (Java 21, Spring Boot, PostgreSQL) with a double-entry ledger in integer minor units; an integration test proves 10 concurrent transfers against a fixed balance never overdraw or create money — see `docs/verification.md` for the measured run.
- Implemented a database-enforced Idempotency-Key contract for transfers (unique constraint + payload hash): identical retries return the original committed transfer and conflicting key reuse returns HTTP 409, verified by automated tests.

## License
MIT — see `LICENSE`. No third-party data is bundled; all accounts/users are fictional and generated by the seeder.
