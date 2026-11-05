-- Historical fees recalled from PHIVE discussions and auction catalogs, not a
-- representation that today's published terms remain unchanged.
-- Store tier details as visible notes. Multi-tier schedules remain inactive
-- because current calculators support only fixed admin + percentage of BID.
ALTER TABLE auction_houses ADD COLUMN tariff_notes TEXT;

INSERT INTO auction_houses(id,name,admin_fee,tax_percent,active,tariff_notes)
VALUES
 ('22222222-2222-4222-8222-222222222228','Car Ready',NULL,NULL,false,
  'Pernah dibahas sebagai sumber unit; belum ditemukan nominal admin atau pajak dari katalog terdahulu. Perlu dokumen tarif.'),
 ('22222222-2222-4222-8222-222222222229','TriBid',NULL,NULL,false,
  'Nama TriBid dibedakan dari Tribik. Tarif admin/pajak belum ditemukan dalam catatan historis yang dapat dipastikan.')
ON CONFLICT DO NOTHING;

UPDATE auction_houses SET tariff_notes=
 'Parameter PHIVE historis: admin Rp3.000.000 + pungutan lelang 1,1% dari harga ketok. Verifikasi kembali aturan lot terbaru.'
WHERE lower(name)='jba';

UPDATE auction_houses SET admin_fee=500000,tax_percent=0,active=false,tariff_notes=
 'Pengguna menjelaskan tarif CARSOME Dealer: harga ketok <Rp100jt biaya Rp500rb; Rp100-200jt Rp750rb; >Rp200jt Rp1jt; tanpa PPN lelang tambahan. Tarif bertingkat belum didukung mesin kalkulasi, jadi balai tetap NONAKTIF. Fiesta lot F5Y00R mendapat PROMO admin GRATIS satu kali, bukan tarif umum.'
WHERE lower(name)='carsome';

UPDATE auction_houses SET admin_fee=3500000,tax_percent=NULL,active=false,tariff_notes=
 'Katalog Otomas yang pernah dianalisis: admin Rp3.500.000 untuk harga ketok sampai Rp400jt; Rp5.500.000 untuk >Rp400-600jt. Di salah satu katalog terdapat pungutan tambahan 0,1% PPN PMK; tidak diasumsikan berlaku universal. Tarif tier dan pajak perlu dikonfirmasi sebelum diaktifkan.'
WHERE lower(name)='otomas';

UPDATE auction_houses SET admin_fee=3500000,tax_percent=NULL,active=false,tariff_notes=
 'Riwayat SmartBID mulai 11 Mei 2026: admin Rp3.500.000 untuk harga ketok <=Rp350jt, >Rp350jt sebesar 1% harga terbentuk. Sebagian lot mencantumkan PPN 1,2%; bukan pajak universal. Tarif tier dan pajak per lot harus dikonfirmasi. Deposit bukan biaya admin.'
WHERE lower(name)='smartbid';

UPDATE auction_houses SET admin_fee=NULL,tax_percent=NULL,active=false,tariff_notes=
 'IBID pernah dibahas dengan admin bervariasi sekitar Rp2.500.000-Rp3.750.000 bergantung unit/KPL. Rp3.500.000 pernah dipakai sebagai ASUMSI analisis, bukan tarif baku semua lot. Pajak perlu dilihat pada katalog atau ketentuan lelang.'
WHERE lower(name)='ibid';

UPDATE auction_houses SET admin_fee=NULL,tax_percent=NULL,active=false,tariff_notes=
 'Tribik pernah dihitung sementara admin Rp3.000.000 + 1,1% sebagai asumsi PHIVE, belum diverifikasi dari katalog. Berbeda dari TriBid.'
WHERE lower(name)='tribik';

UPDATE auction_houses SET tariff_notes='Estimasi terdahulu admin Rp3.000.000 merupakan asumsi perencanaan, bukan tarif terkonfirmasi. Pajak belum diketahui.'
WHERE lower(name)='grosirmobil cisauk';

UPDATE auction_houses SET tariff_notes='Belum ditemukan tarif admin dan pajak terverifikasi dari dokumen historis.'
WHERE lower(name)='bv';
