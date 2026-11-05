-- Fix V11 name alias mismatch: "Caready" normalizes to "caready", not "carready".
-- Existing migrations remain immutable. Historical catalog/won-unit snapshots are untouched.
-- Source: Caready BEKASI-CAR-2611040735 catalog (2026-11-04), page 9.
UPDATE auction_houses SET
 admin_fee=3000000,tax_percent=1.1,active=true,
 tariff_rule='{"feeRule":"STEP_TIERS","taxPolicy":"CONDITIONAL_MAX","tiers":[{"upToBid":400000000,"adminFee":3000000},{"upToBid":600000000,"adminFee":5000000},{"upToBid":800000000,"adminFee":8000000},{"upToBid":1000000000,"adminFee":10000000},{"upToBid":1200000000,"adminFee":12000000}],"stepSize":200000000,"stepAdminIncrease":2000000}'::jsonb,
 tariff_notes='DARI PDF Caready Bekasi 4 Nov 2026 hlm 9: Admin mobil <=400jt Rp3jt; >400-600jt Rp5jt; >600-800jt Rp8jt; >800jt-1M Rp10jt; >1M-1,2M Rp12jt; kelipatan 200jt selanjutnya tambah Rp2jt. Tambahan PPN 1,1% HANYA lot yang diberi penanda, bukan seluruh unit. Dalam estimasi umum angka 1,1% ialah cadangan konservatif, bukan tagihan pasti. Deposit mobil Rp5jt per unit, biasanya dikembalikan bila tidak menang. Pelunasan maksimal 4 hari kerja. Batas pengambilan 7 hari, setelahnya denda yang belum diketahui nominalnya. Biaya per lot misalnya mobilisasi Rp777rb: baca catatan masing-masing lot. Gunakan katalog final pada hari lelang.',
 updated_at=now()
WHERE lower(regexp_replace(name,'[^a-zA-Z]','','g')) IN ('caready','carready');
