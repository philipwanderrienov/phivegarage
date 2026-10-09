CREATE TABLE hunter_listings (
 id UUID PRIMARY KEY,
 source TEXT NOT NULL CHECK (source IN ('FACEBOOK_MANUAL','OLX_MANUAL','AUCTION_MANUAL','OTHER_MANUAL','LICENSED_FEED')),
 source_url TEXT,
 title TEXT NOT NULL,
 brand TEXT NOT NULL,
 model TEXT NOT NULL,
 manufacture_year INTEGER CHECK (manufacture_year BETWEEN 1950 AND 2100),
 asking_price BIGINT NOT NULL CHECK (asking_price > 0),
 kilometer INTEGER CHECK (kilometer >= 0),
 location TEXT,
 transmission TEXT,
 stnk_status TEXT NOT NULL DEFAULT 'UNKNOWN' CHECK (stnk_status IN ('ADA','TIDAK_ADA','UNKNOWN')),
 bpkb_status TEXT NOT NULL DEFAULT 'UNKNOWN' CHECK (bpkb_status IN ('ADA','TIDAK_ADA','UNKNOWN')),
 listing_status TEXT NOT NULL DEFAULT 'UNVERIFIED' CHECK (listing_status IN ('UNVERIFIED','ACTIVE','SOLD','REMOVED')),
 verified_at TIMESTAMPTZ,
 imported_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 notes TEXT NOT NULL DEFAULT '',
 UNIQUE (source,source_url)
);
CREATE TABLE hunter_comparables (
 id UUID PRIMARY KEY,
 listing_id UUID NOT NULL REFERENCES hunter_listings(id) ON DELETE CASCADE,
 source_url TEXT,
 price BIGINT NOT NULL CHECK(price > 0),
 verified_at TIMESTAMPTZ,
 sold_confirmed BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX hunter_listings_filter_idx ON hunter_listings(brand,asking_price,listing_status);
CREATE INDEX hunter_comparables_listing_idx ON hunter_comparables(listing_id);
