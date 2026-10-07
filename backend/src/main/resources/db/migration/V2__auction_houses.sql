-- JBA tariff supplied by user: fixed admin IDR 3,000,000 + 1.1% of BID.
CREATE TABLE auction_houses (
 id UUID PRIMARY KEY, name VARCHAR(120) NOT NULL, admin_fee BIGINT NOT NULL CHECK(admin_fee>=0 AND admin_fee<=100000000000),
 tax_percent NUMERIC(7,4) NOT NULL CHECK(tax_percent>=0 AND tax_percent<=100),
 active BOOLEAN NOT NULL DEFAULT true, updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX auction_houses_name_idx ON auction_houses(lower(name));
INSERT INTO auction_houses(id,name,admin_fee,tax_percent) VALUES ('11111111-1111-4111-8111-111111111111','JBA',3000000,1.1);
ALTER TABLE catalogs ADD COLUMN auction_house_id UUID REFERENCES auction_houses(id) ON DELETE RESTRICT;
ALTER TABLE catalogs ADD COLUMN fee_snapshot JSONB;
-- Do not invent fees for other/legacy houses; the user must select a configured house.
UPDATE catalogs SET auction_house_id='11111111-1111-4111-8111-111111111111',
 fee_snapshot='{"name":"JBA","adminFee":3000000,"taxPercent":1.1,"taxBasis":"BID"}'::jsonb WHERE lower(trim(house))='jba';
