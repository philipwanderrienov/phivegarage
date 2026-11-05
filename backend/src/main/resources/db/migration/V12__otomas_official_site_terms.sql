-- Otomas official public site reviewed 2026-11-05:
-- https://balailelangotomas.co.id/p/live-auction
-- Site explicitly requires hammer price PLUS administration but does not state
-- numerical VAT / PPN on auction wins. Never infer 0% or 1.1%.
-- The latest uploaded Oct 26 catalog establishes the fixed tier bands in V11.
-- Website deposit 5m per lot versus PDF high-value 25m: treat as event-specific.
UPDATE auction_houses
SET tax_percent=NULL,active=false,
    tariff_notes=concat_ws(E'\n',tariff_notes,
      'CEK WEBSITE RESMI 5 NOV 2026 — https://balailelangotomas.co.id/p/live-auction : Pemenang wajib bayar harga ketok + admin sesuai ketentuan, tetapi tarif PPN lelang TIDAK disebut. Jangan asumsikan PPN 0% atau 1,1%. Syarat situs: deposit Rp5jt/lot, pelunasan maksimal 3 hari kerja; deposit hangus jika wanprestasi, penitipan harian terlambat dikenakan biaya tanpa nominal tertera; beban balik nama/peralihan hak pembeli. Perbedaan deposit terhadap PDF Okt 2026 (Rp25jt untuk unit >Rp300jt) harus dicek per event. Admin tetap mengikuti tingkatan pada PDF hlm 7.'),
    updated_at=now()
WHERE lower(name)='otomas';
