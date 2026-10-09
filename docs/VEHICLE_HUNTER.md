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
