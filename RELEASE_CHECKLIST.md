# Release checklist
- [ ] `./mvnw -B verify` green against a real PostgreSQL
- [ ] `scripts/smoke-test.sh` passes against a fresh local instance
- [ ] Persistence checked: restart app (or `docker compose restart app`) and confirm statements remain
- [ ] No secrets, resumes, or employer material in the repo (`git grep -i password` reviewed; only documented demo credentials)
- [ ] README startup commands re-run from a clean clone
- [ ] Screenshot + demo recording captured (currently PENDING)
- [ ] Public deployment: NOT done — requires an authorized VM/account, HTTPS proxy, changed DB password, demo seeder removed
- [ ] Tag `v1.0.0` only after all executed checks above pass
