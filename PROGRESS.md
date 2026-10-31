# Progress

- [x] Milestone 1 — Vertical slice (code complete, compile-verified): Spring Boot app, Flyway schema, seeded demo users, accounts/transfer/statement UI + API. Runtime execution in this sandbox was blocked by the Java-TCP restriction documented in docs/verification.md.
- [x] Milestone 2 — Core behavior: double-entry ledger, integer minor units, idempotency-key (DB unique + payload hash), ordered pessimistic account locking, ownership checks.
- [x] Milestone 3 — Tests: unit hash test; PostgreSQL integration tests (transfer, idempotent retry, payload conflict, insufficient funds, unauthorized debit, concurrency); MockMvc security tests.
- [x] Milestone 4 — Deployment config: Dockerfile + docker-compose.yml (app + PostgreSQL, persistent volume). Docker is NOT installed in the build sandbox, so compose was not executed here — see docs/verification.md.
- [ ] Milestone 5 — Portfolio evidence: screenshot + 60–90s demo recording PENDING (script in docs/demo.md). No screenshot is included because none was genuinely captured in this sandbox.
- [ ] Public deployment: PENDING — no deployment account authorized. Route documented in README (single VM + Compose + HTTPS reverse proxy).

Environment note: this sandbox had no Java/Maven/Docker/PostgreSQL initially. `apt-get update` **failed** (exit 100 — Ubuntu mirror fetch failed through the sandbox egress proxy), so JDK 21 (Temurin 21.0.13) and Maven 3.9.9 were installed from official tarballs instead, and PostgreSQL was never installed. Maven dependencies were pre-seeded into a local repository with curl and the build run offline, because direct TCP from Java processes is blocked in this sandbox (see docs/verification.md for the measured evidence). Main code and all test code **compile**; the DB-free unit test result and the blocked DB-backed checks are recorded in docs/verification.md.
