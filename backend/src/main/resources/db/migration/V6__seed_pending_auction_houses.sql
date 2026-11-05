-- Master houses previously considered for PHIVE Garage. The active spreadsheet
-- only documents Carsome's first-purchase fee waiver; it does not establish
-- permanent tariffs for these houses. NULL means "not verified", NOT free.
ALTER TABLE auction_houses ALTER COLUMN admin_fee DROP NOT NULL;
ALTER TABLE auction_houses ALTER COLUMN tax_percent DROP NOT NULL;
ALTER TABLE auction_houses ADD CONSTRAINT auction_houses_active_tariff_required
 CHECK (NOT active OR (admin_fee IS NOT NULL AND tax_percent IS NOT NULL));

INSERT INTO auction_houses(id,name,admin_fee,tax_percent,active)
VALUES
 ('22222222-2222-4222-8222-222222222221','CARSOME',NULL,NULL,false),
 ('22222222-2222-4222-8222-222222222222','IBID',NULL,NULL,false),
 ('22222222-2222-4222-8222-222222222223','Otomas',NULL,NULL,false),
 ('22222222-2222-4222-8222-222222222224','Smartbid',NULL,NULL,false),
 ('22222222-2222-4222-8222-222222222225','Tribik',NULL,NULL,false),
 ('22222222-2222-4222-8222-222222222226','Grosirmobil Cisauk',NULL,NULL,false),
 ('22222222-2222-4222-8222-222222222227','BV',NULL,NULL,false)
ON CONFLICT DO NOTHING;
