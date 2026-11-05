-- Carsome's historical tier boundaries and 0% auction tax were supplied in
-- prior PHIVE discussion; make available for use only with the tier-aware
-- calculator introduced in this release. Existing Fiesta snapshots stay intact.
UPDATE auction_houses SET active=true,updated_at=now()
WHERE lower(name)='carsome' AND admin_fee=500000 AND tax_percent=0;
