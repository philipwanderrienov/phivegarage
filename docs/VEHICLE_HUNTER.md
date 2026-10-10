# Vehicle Hunter — v1

Branch: `feature/vehicle-hunter`.

## Scope currently implemented
- Vue menu **Vehicle Hunter** in the existing sidebar; manual candidate creation and original-source links.
- Listing source types: FACEBOOK_MANUAL, OLX_MANUAL, AUCTION_MANUAL, OTHER_MANUAL, LICENSED_FEED.
- Source timestamps, documents statuses, prices and optional comparable references stored in PostgreSQL with Flyway V5.
- GET/POST `/api/hunter/listings`; POST `/api/hunter/listings/{id}/comparables`; GET `/api/hunter/listings/{id}/evaluate`.
- Financial evaluation is **deterministic**, not an AI prediction: budget, target profit, repair cost, tax/documents, transport/ads, and risk reserve must all be entered by the user. No hidden monetary defaults; buyer target (retail/dealer) is selectable.
- Conservative resale estimate = **lowest entered comparable asking price** × 95% for retail or 85% for dealer, with floor rounding.
- `maxBuyPrice = max(0, min(budget - otherCosts, quickSaleEstimate - targetProfit - otherCosts))`.
- BUY only when asking price is within maxBuyPrice, STNK and BPKB marked ADA, and comparable evidence exists. Unknown documents or no comparables => REVIEW. Inactive listings => SKIP.
- No invented listings, no automatic scraping, no bypassing Meta restrictions. Input is user supplied and **not independently verified**.

## Not yet implemented
- Licensed external provider ingestion, API credential management, lawful crawling or scheduled synchronization.
- Automatic search for market comparables, sold-price verification, AI photo inspection, image storage, deduplication across providers, liquidity score calibration, offer tracking.
- The listing can now be sent to the existing Purchase Decision form with the current inputs prefilled. Price evidence and physical inspection are **not automatically verified**.
- Automated integration tests and CI validation for the new module.

## Example

```http
POST /api/hunter/listings
Content-Type: application/json

{
 "source":"FACEBOOK_MANUAL",
 "sourceUrl":"https://www.facebook.com/marketplace/item/EXAMPLE",
 "title":"Toyota Vios 2004 manual",
 "brand":"Toyota",
 "model":"Vios",
 "year":2004,
 "askingPrice":43000000,
 "stnk":"ADA",
 "bpkb":"ADA",
 "location":"Bekasi"
}
```

After receiving its id, add two or more documented listing comparables through `POST /api/hunter/listings/{id}/comparables`, then call `GET /api/hunter/listings/{id}/evaluate?budget=50000000&targetProfit=8000000` with values provided by the user; all six monetary query parameters are required; omitted parameters return HTTP 400. Example URL above is a placeholder, **not a real listing**.

Security: all endpoints inherit the existing HTTP Basic authentication and `X-PhiveGarage: web` header. No third-party credentials or private Facebook session cookies are stored.

## Dynamic inputs and handoff

Vehicle Hunter requires users to enter budget, target profit, repair allowance, vehicle tax/document costs, transport/ads and risk reserve before running an evaluation. Changing an input clears stale evaluation results until the user recalculates. The **Lanjut ke Keputusan Beli** action populates the existing Purchase Decision form. The user must still confirm comparable evidence, documents and inspection; Hunter heuristics do not automatically set these flags.

## Validation checklist

Run `mvn -f backend/pom.xml verify` to execute VehicleHunterControllerTest (dynamic budget, target profit, variable repairs, missing comparables, invalid input). Run `cd frontend && npm ci && npm run build`. Smoke-test: create candidate, supply all six monetary inputs, add comparable prices, calculate, change one cost (previous result should clear), calculate again, and click **Lanjut ke Keputusan Beli**. Ensure final purchase report is not treated as independently verified. Automated build and deployment are not yet confirmed.

## Meta API review (2026-10-09)

The documented Meta Catalog Management endpoints (`/{business-id}/owned_product_catalogs`, commerce catalog APIs) manage catalogs accessible to an authorized business, not arbitrary public vehicle listings in Facebook Marketplace. No official general-purpose Marketplace search/listing endpoint was verified on Meta for Developers. Third-party APIs such as Social Fetch and ScrapeAtlas advertise Marketplace search; they are **not official Meta APIs**, and their licensing, legality for this application, coverage of Jabodetabek, prices and reliability must be independently checked before any integration. Do not store Facebook session cookies or bypass access restrictions.

## Listing verification gate

PATCH `/api/hunter/listings/{id}/status` with JSON `{"status":"ACTIVE"}`, `UNVERIFIED`, `SOLD`, or `REMOVED`. ACTIVE is user-attested, never a claim of Meta confirmation. Unverified listings cannot receive BUY; SOLD/REMOVED are SKIP. The UI exposes the manual status selector and discards stale analysis on changes.

## Hunting keyword priorities

The default keyword set is: **BU**, **Butuh Uang**, **Butuh dana cepat**, **Jual cepat**, **Jual rugi**, **Pemakaian pribadi**, **Atas nama pribadi**. All seven are selectable independently in the Vehicle Hunter UI. A keyword-only toggle filters stored candidates; matched phrases are displayed and results are sorted by maximum keyword priority. Backend matching is case-insensitive, whitespace-normalized, and uses token boundaries so BU does not match fragments inside longer words.

Urgency claims (BU, Butuh Uang, Butuh dana cepat, Jual cepat) rank first for *review*, followed by Jual rugi, then private-use/ownership claims. This rank is only a discovery hint: it does not alter price calculations, bypass documentation checks, prove ownership, or imply the seller truly needs cash.

The keyword ↗ action performs a user-triggered ordinary Google search (keyword plus typed vehicle/location text). It does not scrape websites, save search results automatically, or claim provider access. When a licensed data connector is added, these exact keyword signals should be reused for search-query generation and incoming listing classification.

### Additional hunting signals
- **Milik pribadi** — ownership claim (priority 1).
- **Lanjut rawat** / **Lanjut ngerawatin** — handover wording (priority 2).
- **Nerusin** / **Terusin** — colloquial handover wording (priority 2).
Each variant is an independent selectable keyword. Matching continues to respect word boundaries and does not treat these phrases as verified ownership or a guaranteed bargain.
