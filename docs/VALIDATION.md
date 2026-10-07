# Validation — 7 October 2026

- Java 17 / Maven `verify`: PASS, backend compiled and executable Spring Boot JAR packaged.
- JUnit: 9 tests PASS (fee rounding, capital/profit cap, impossible deal, price evidence gate, verification gate, high repair estimate).
- Additional Java calculator check: 160 sale/fee/capital scenarios PASS.
- Vue production build: PASS (`npm run build`).
- PostgreSQL DDL: PASS in PGlite (PostgreSQL WASM), tables `catalogs`, `lots`, `analyses` created.
- Browser checks with mocked API fixture: PASS login, catalog selection, lot edit/save, watchlist and detail modal; no page JS errors.
- Desktop 1440px / mobile 390px screenshots inspected. No document-wide horizontal overflow; tables scroll inside their containers.

Not yet validated against a running external PostgreSQL server or live OpenAI API/PDF. No user API key or real auction PDF was supplied. Live integration smoke-test steps are in `SMOKE_TEST.md`.

Initial GitHub Actions run `37595472513` ended in failure before any job steps ran; jobs exposed no step logs. Local builds/tests above completed successfully. Check the repository Actions page for runner/account availability before relying on CI status.
