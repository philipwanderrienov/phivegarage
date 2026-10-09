# Vehicle Hunter — v1

Branch: `feature/vehicle-hunter`.

## Scope currently implemented
- Vue menu **Vehicle Hunter** in the existing sidebar; manual candidate creation and original-source links.
- Listing source types: FACEBOOK_MANUAL, OLX_MANUAL, AUCTION_MANUAL, OTHER_MANUAL, LICENSED_FEED.
- Source timestamps, documents statuses, prices and optional comparable references stored in PostgreSQL with Flyway V5.
- GET/POST `/api/hunter/listings`; POST `/api/hunter/listings/{id}/comparables`; GET `/api/hunter/listings/{id}/evaluate`.
- Financial evaluation is **deterministic**, not an AI prediction: default cash budget Rp50m, minimum profit Rp8m, repairs Rp2.5m, taxes Rp1.5m, transport Rp0.5m, risk reserve Rp1.5m. Values for the last four costs are API query parameters; the first two are also adjustable in the UI.
- Conservative resale estimate = **lowest entered comparable asking price** × 95% for retail or 85% for dealer, with floor rounding.
- `maxBuyPrice = max(0, min(budget - otherCosts, quickSaleEstimate - targetProfit - otherCosts))`.
- BUY only when asking price is within maxBuyPrice, STNK and BPKB marked ADA, and comparable evidence exists. Unknown documents or no comparables => REVIEW. Inactive listings => SKIP.
- No invented listings, no automatic scraping, no bypassing Meta restrictions. Input is user supplied and **not independently verified**.

## Not yet implemented
- Licensed external provider ingestion, API credential management, lawful crawling or scheduled synchronization.
- Automatic search for market comparables, sold-price verification, AI photo inspection, image storage, deduplication across providers, liquidity score calibration, offer tracking.
- Directly transferring the selected hunter listing into the existing Purchase Decision module. For now, fill Purchase Decision manually after evaluating.
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

After receiving its id, add two or more documented listing comparables through `POST /api/hunter/listings/{id}/comparables`, then call `GET /api/hunter/listings/{id}/evaluate?budget=50000000&targetProfit=8000000`. Example URL above is a placeholder, **not a real listing**.

Security: all endpoints inherit the existing HTTP Basic authentication and `X-PhiveGarage: web` header. No third-party credentials or private Facebook session cookies are stored.
