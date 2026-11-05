-- Sourced from four uploaded PDFs; this is NOT an unconditional lot tax.
-- Tax 1.1% for Tribik, SmartBID and Caready is a conservative maximum:
-- apply only to lots explicitly marked. Automatic analysis may overreserve it.
-- Do not overwrite existing catalog / acquisition fee snapshots.
-- Tribik: DAFTAR LOT 3 NOVEMBER 2026 (EVENT JAKARTA) FINAL, p.1.
UPDATE auction_houses SET
 admin_fee=3000000,tax_percent=1.1,active=true,
 tariff_rule='{"feeRule":"STEP_TIERS","taxPolicy":"CONDITIONAL_MAX","tiers":[{"upToBid":300000000,"adminFee":3000000},{"upToBid":500000000,"adminFee":4000000},{"upToBid":600000000,"adminFee":5000000},{"upToBid":700000000,"adminFee":8000000},{"upToBid":900000000,"adminFee":9000000},{"upToBid":1000000000,"adminFee":10000000}],"stepSize":200000000,"stepAdminIncrease":2000000}'::jsonb,
 tariff_notes='TERVERIFIKASI DARI PDF Tribik 3 Nov 2026 hlm 1: admin bertingkat 3jt (<=300jt), 4jt (>300-500jt), 5jt (>500-600jt), 8jt (>600-700jt), 9jt (>700-900jt), 10jt (>900jt-1M), kemudian naik 2jt setiap 200jt. PPN 1,1% HANYA LOT TERTENTU, bukan semua unit. Kalkulasi umum memakai buffer konservatif 1,1%; konfirmasi flag lot. Deposit 5jt unit <300jt, 25jt unit >300jt; Rp300jt persis perlu konfirmasi. Parkir terlambat 100rb/hari setelah H+5 nonmember, H+7 member (wilayah tertentu H+5). BBN/ETLE dan perbaikan menjadi tanggung jawab pembeli. Batas pelunasan 3 hari kerja. Periksa daftar final hari-H.',
 updated_at=now()
WHERE lower(name)='tribik';
-- SmartBID: Katalog Lelang Mobil BEKASI 05-11-2026 (1), p.1.
UPDATE auction_houses SET
 admin_fee=3500000,tax_percent=1.1,active=true,
 tariff_rule='{"feeRule":"SMARTBID_2026","taxPolicy":"CONDITIONAL_MAX"}'::jsonb,
 tariff_notes='TERVERIFIKASI DARI PDF SmartBID Bekasi 5 Nov 2026 hlm 1: biaya admin 3,5jt per unit s.d. harga ketok 350jt, dan 1% di atas 350jt. PPN 1,1% HANYA LOT BERTANDA; kalkulasi umum mengalokasikan buffer konservatif 1,1%. Sebagian lot memiliki roya fidusia 100rb, mobilisasi 350rb atau 2,5jt, dan biaya lain: jangan jadikan semua sebagai biaya tetap. Batas pembayaran lot bervariasi, 1/3/5 hari setelah invoice; periksa masing-masing.',
 updated_at=now()
WHERE lower(name)='smartbid';
-- Otomas: Daftar Lot Sementara 1 L636(1), p.7.
-- Tax omitted: do not turn silence into 0% or invent 1.1%.
UPDATE auction_houses SET
 admin_fee=3500000,tax_percent=NULL,active=false,
 tariff_rule='{"feeRule":"STEP_TIERS","taxPolicy":"UNKNOWN","tiers":[{"upToBid":400000000,"adminFee":3500000},{"upToBid":600000000,"adminFee":5500000},{"upToBid":800000000,"adminFee":8500000}],"stepSize":200000000,"stepAdminIncrease":2000000}'::jsonb,
 tariff_notes='TERVERIFIKASI DARI PDF Otomas 26 Okt 2026 hlm 7: admin <=400jt 3,5jt; >400-600jt 5,5jt; >600-800jt 8,5jt; berikutnya +2jt tiap 200jt. PDF tidak mencantumkan tarif PPN lelang umum; JANGAN ASUMSI PAJAK 0%. Deposit 5jt (<=300jt) atau 25jt (>=300jt, batas 300jt persis ambigu), dikembalikan jika tidak menang; hangus jika wanprestasi. Wajib pelunasan dan cek ketentuan hari lelang. Tarif admin dapat dikonfirmasi, namun perlu klarifikasi pajak sebelum dipakai analisis otomatis.',
 updated_at=now()
WHERE lower(name)='otomas';
-- Caready: Katalog BEKASI-CAR-2611040735 SEMENTARA, p.9.
UPDATE auction_houses SET
 name='Caready',admin_fee=3000000,tax_percent=1.1,active=true,
 tariff_rule='{"feeRule":"STEP_TIERS","taxPolicy":"CONDITIONAL_MAX","tiers":[{"upToBid":400000000,"adminFee":3000000},{"upToBid":600000000,"adminFee":5000000},{"upToBid":800000000,"adminFee":8000000},{"upToBid":1000000000,"adminFee":10000000},{"upToBid":1200000000,"adminFee":12000000}],"stepSize":200000000,"stepAdminIncrease":2000000}'::jsonb,
 tariff_notes='TERVERIFIKASI DARI PDF Caready Bekasi 4 Nov 2026 hlm 9: admin mobil <=400jt 3jt; >400-600jt 5jt; >600-800jt 8jt; >800jt-1M 10jt; >1M-1,2M 12jt; tiap 200jt selanjutnya +2jt. PPN tambahan 1,1% HANYA LOT BERTANDA; kalkulasi umum mengalokasikan buffer konservatif. Deposit mobil 5jt/unit (dikembalikan jika tidak menang). Pelunasan 4 hari kerja. Ambil unit <=7 hari, lewatnya dikenakan denda yang nominalnya tidak tercantum. Beberapa lot dikenakan mobilisasi 777rb, 388.500, 555rb dan biaya dokumen lain. Patuhi keterangan per lot dan daftar final.',
 updated_at=now()
WHERE lower(replace(name,' ',''))='carready';
