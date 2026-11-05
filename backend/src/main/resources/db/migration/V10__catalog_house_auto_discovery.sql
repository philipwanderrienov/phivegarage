-- Auto-discovered tariff evidence remains tied to each uploaded PDF.
ALTER TABLE catalogs ADD COLUMN house_detection JSONB;
ALTER TABLE auction_houses ADD COLUMN tariff_rule JSONB;
-- A catalog may be uploaded without choosing a house in advance.
-- Existing links, historical snapshots, and acquired-unit data are unchanged.
