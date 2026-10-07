# Validation — 7 October 2026

- Java 17 / Maven `verify`: PASS, backend compiled and executable Spring Boot JAR packaged.
- JUnit: 22 tests PASS (fee rounding, capital/profit cap, impossible deal, price evidence gate, verification gate, high repair estimate, missing-fact REVIEW, cost reconciliation, structured-output validation, corrupt/password PDF rejection).
- Additional Java calculator check: 160 sale/fee/capital scenarios PASS.
- Vue production build: PASS (`npm run build`).
- PostgreSQL DDL: PASS in PGlite (PostgreSQL WASM), tables `catalogs`, `lots`, `analyses` created.
- Browser checks with mocked API fixture: PASS login, catalog selection, lot edit/save, watchlist and detail modal; no page JS errors.
- Desktop 1440px / mobile 390px screenshots inspected. No document-wide horizontal overflow; tables scroll inside their containers.

Not yet validated against a running external PostgreSQL server or live OpenAI API/PDF. No user API key or real auction PDF was supplied. Live integration smoke-test steps are in `SMOKE_TEST.md`.

Development testing now runs directly on the user's server. No GitHub Actions workflow is installed. The earlier Actions jobs did not start because GitHub reported an account billing lock; this does not affect the successful local builds above. Follow `SMOKE_TEST.md` for server validation.

MVP refinement: failed-catalog retry UI and cost details tested with API fixtures. Retry backend/database/OpenAI integration still needs a server smoke test. No database migration is required for this refinement; cost details are saved in existing JSON analysis results. Historical analyses without cost fields remain readable.
