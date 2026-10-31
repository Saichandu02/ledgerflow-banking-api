# Verification

Build environment (this sandbox): Ubuntu 24.04, 4 cores, ~8 GB RAM. **No Java, Maven, Docker, or PostgreSQL was preinstalled.**

## Toolchain installed for this build
- Temurin JDK **21.0.13+9** (official Eclipse Adoptium tarball) — `java -version` verified: *OpenJDK Runtime Environment Temurin-21.0.13+9*.
- Apache Maven **3.9.9** (official Apache tarball) — `mvn -version` verified against JDK 21.
- `apt-get` **failed**: `apt-get update` exited 100 — *"Failed to fetch http://mirror.cogentco.com/pub/linux/ubuntu/dists/noble-security/... Connection failed"* through the sandbox egress proxy. So **PostgreSQL was never installed** and **Docker is not present**. `Dockerfile` / `docker-compose.yml` are included as configuration only and were **NOT executed** here.
- Maven wrapper: `mvnw`/`mvnw.cmd` scripts plus the official `maven-wrapper-3.2.0.jar` and properties (Maven 3.9.9) are committed; the wrapper itself was not exercised end-to-end in this sandbox (it downloads Maven over the network, which Java cannot do here — see below).

## Network limitation found (important, measured)
Direct TCP from Java processes is blocked in this sandbox by a permission layer. Evidence, all reproduced in this session:
- Maven (online) fetching from Maven Central fails: *"Unsupported or unrecognized SSL message"* — the blocker's plain-text response to a TLS handshake.
- A minimal Java program opening a socket to `127.0.0.1:9999` (a Python HTTP server that curl reached with HTTP 200) received this literal response instead: *"muse: Other TCP connections is turned off for this assistant. To allow it, ask the user to open Muse settings -> Permissions -> Direct network protocols and switch other_tcp from Deny to Ask."*
- A Spring integration test (below) failed at Flyway with `org.postgresql.util.PSQLException: An error occurred while setting up the SSL connection` — the PostgreSQL driver reading the same plain-text denial during its SSL handshake.
- Shell tools (curl/Python) reach the internet fine through the sanctioned egress proxy, and the JDK/Maven tarballs and every Maven artifact were downloaded that way.

Workaround used: all Maven artifacts (~170 jars) were **pre-seeded into a local repository with curl** (including parent POMs and imported BOMs, which Maven's non-debug errors don't name), and every build below was run **offline** (`mvn -o -Dmaven.repo.local=…`). This validates compilation and DB-free tests; it cannot validate JDBC behavior.

## What was actually executed — results

| Check | Command | Result |
|---|---|---|
| Compile (main) | `mvn -o -B compile -DskipTests` | ✅ **BUILD SUCCESS** (2.9 s; 23 class files) |
| Compile (tests) | `mvn -o -B test-compile` | ✅ **BUILD SUCCESS** — all 3 test classes compile |
| Package | `mvn -o -B package -DskipTests` | ✅ **BUILD SUCCESS** — executable Spring Boot jar built (61,249,984 bytes; manifest `Main-Class: JarLauncher`, `Start-Class: dev.ledgerflow.LedgerFlowApplication`; 87 dependency jars in `BOOT-INF/lib`). The jar itself was not *run* — startup needs the blocked JDBC connection. |
| Unit test | `mvn -o -B test -Dtest=TransferServiceUnitTest` | ✅ **Tests run: 1, Failures: 0, Errors: 0, Skipped: 0** (0.207 s) — SHA-256 idempotency payload hash is deterministic and payload-sensitive |
| Spring context + DB test | `mvn -o -B test -Dtest=ApiSecurityTest#healthIsPublic` | ❌ **Error (blocked, measured)**: context start reached Flyway, then `FlywaySqlException: Unable to obtain connection from database` → `PSQLException: An error occurred while setting up the SSL connection` (the Java-TCP block above; there is also no PostgreSQL server in this sandbox) |
| Integration tests (7, `TransferIntegrationTest`) | `mvn verify` against real PostgreSQL | ⛔ **NOT RUN** — require a live PostgreSQL over JDBC; blocked as above. They compile. |
| App run + HTTP smoke test | `mvn spring-boot:run` / `scripts/smoke-test.sh` | ⛔ **NOT RUN** — the app needs its datasource connection at startup; blocked as above. |
| Docker Compose | `docker compose up` | ⛔ **NOT RUN** — Docker not installed; apt failed. |

### Bugs found *because* tests were executed
1. `TransferIntegrationTest` imported `PlatformTransactionManager` from a non-existent package — caught by `test-compile`, fixed.
2. `application.yml` used YAML flow style containing `${PORT:8080}` — SnakeYAML failed with *"while parsing a flow mapping"* and the Spring context would not load **anywhere**. Caught by running `ApiSecurityTest`; rewritten in block style and the re-run progressed past YAML parsing to the (sandbox-only) DB connection failure.
3. `TransferService.ApiException` thrown from the web layer had no HTTP status mapping — a `@ControllerAdvice` handler was added.

## How to complete the remaining verification on a normal machine
```bash
docker compose up --build            # or install PostgreSQL 16 locally
createdb -O ledgerflow ledgerflow_test   # for the test profile
./mvnw -B verify                     # full suite incl. real-PostgreSQL integration tests
bash scripts/smoke-test.sh           # against the running app: idempotent 2500-minor transfer sent twice
```
Expected from the test design (**not** from an executed run): 2500 minor units move exactly once (alice 100000 → 97500), 10 concurrent 20000-minor transfers against a 100000 balance yield exactly 5 successes and a 0 balance with reconciliation intact, insufficient funds returns 422, another user's debit returns 403, same key + different payload returns 409.

## To unblock this sandbox specifically
A user with access to Muse settings can switch **Settings → Permissions → Direct network protocols → other_tcp** from *Deny* to *Ask*, install PostgreSQL (or Docker), create the `ledgerflow` / `ledgerflow_test` databases per the README, and re-run the commands above.
