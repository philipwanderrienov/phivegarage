# Validation — 7 October 2026

- Java 17 / Maven `verify`: PASS, backend compiled and executable Spring Boot JAR packaged.
- JUnit: 40 tests PASS (fee rounding, capital/profit cap, impossible deal, price evidence gate, verification gate, high repair estimate, missing-fact REVIEW, cost reconciliation, structured-output validation, corrupt/password PDF rejection).
- Additional Java calculator check: 160 sale/fee/capital scenarios PASS.
- Vue production build: PASS (`npm run build`).
- PostgreSQL DDL: PASS in PGlite (PostgreSQL WASM), tables `catalogs`, `lots`, `analyses` created.
- Browser checks with mocked API fixture: PASS login, catalog selection, lot edit/save, watchlist and detail modal; no page JS errors.
- Desktop 1440px / mobile 390px screenshots inspected. No document-wide horizontal overflow; tables scroll inside their containers.

Not yet validated against a running external PostgreSQL server or live OpenAI API/PDF. No user API key or real auction PDF was supplied. Live integration smoke-test steps are in `SMOKE_TEST.md`.

Development testing now runs directly on the user's server. No GitHub Actions workflow is installed. The earlier Actions jobs did not start because GitHub reported an account billing lock; this does not affect the successful local builds above. Follow `SMOKE_TEST.md` for server validation.

MVP refinement: failed-catalog retry UI and cost details tested with API fixtures. Retry backend/database/OpenAI integration still needs a server smoke test. The previous cost/retry refinement required no database migration; cost details are saved in existing JSON analysis results. Historical analyses without cost fields remain readable.

Auction-house update: V1→V2 DDL, JBA seed, legacy JBA backfill, snapshot preservation after master edit, and referenced-house deletion restriction PASS in PostgreSQL WASM. House tariff/cap/STNK filters covered by Java tests. Vue build PASS. Browser fixture checks cover house create/edit/delete/deactivate selected house ID on upload, ALL IN column, maximum bid input, and ALL IN preview; no page errors or mobile overflow. Actual Java JDBC CRUD + live AI still require server testing.


Won-unit update (2026-10-07): Maven `verify` PASS (40 tests total) and Vue production build PASS. Nine new Java tests cover actual/ideal acquisition, expenses/margins, null unsold margins, losses/rounding, sale-date validation, sold-only funds, expense ownership scope, catalogue fee snapshot selection, and immutable house/source. Controller tests use mocked JDBC, not a live database.

V1→V3 SQL PASS in PostgreSQL WASM: default starting funds, unique source lot, protected house/lot references, positive expense amount, expense deletion cascade. Browser API-fixture checks PASS: manual won-unit create/edit/delete, expense create/edit/delete, target/real margins, sale, funds, catalogue Catat menang prefill and locked house/source, mobile width (390px), no page errors. Fixtures are synthetic and contain no spreadsheet records.

Java JDBC + PostgreSQL persistence/restart and real server authentication remain pending server smoke tests. The source spreadsheet was read for formulas; it was not edited and its private inventory was not copied into the repository.
