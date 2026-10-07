# Smoke test server

1. Jalankan PostgreSQL dan backend (Flyway membuat tiga tabel), frontend melalui Vite atau Nginx.
2. `/api/health` menghasilkan UP. `/api/catalogs` tanpa auth harus 401. Dengan auth tanpa X-PhiveGarage harus 403.
3. Login salah ditolak; login benar menampilkan katalog kosong. Pastikan key tidak terdapat pada network response/config atau JS bundle.
4. Upload non-PDF dan PDF >20 MB ditolak. Upload PDF kecil 1–3 halaman dengan 2–3 lot yang bisa diperiksa manual. Status QUEUED → EXTRACTING → READY.
5. Cocokkan jumlah lot, harga, nomor, surat dan sourcePage terhadap PDF; unknown tetap null, tidak fabricated.
6. Analisis tanpa verifikasi/pembanding menghasilkan REVIEW dan max bid kosong. Tidak ada BID berdasarkan estimasi AI saja.
7. Edit satu lot dengan data lengkap/verified dan pembanding. Analisis ulang menghasilkan snapshot baru. Hasil lama tetap tersedia.
8. Contoh: pembanding 80jt, retail => sale 76jt; biaya nonbid 8jt, fixed fee 2jt, target laba 5jt, modal 65jt, fee 0% => max bid 55jt. Base 50jt => total 60jt, laba 16jt. Base 56jt => SKIP.
9. Ubah fee ke 2%; hitung max bid dengan rumus README, pastikan total di max bid tidak melebihi 65jt.
10. Simpan unit ke bid board, refresh katalog/analisis: flag harus tersimpan. Export JSON berisi snapshot parameter/hasil yang dipilih.
11. Hentikan backend di tengah pekerjaan; restart harus menunjukkan FAILED dan tindakan re-upload/re-analyze dapat dilakukan.
12. Uji tampilan 1440px dan 390px: tabel boleh scroll horizontal, form/sidebar tidak keluar viewport.

Live AI membutuhkan OPENAI_API_KEY milik operator, panggilan berbayar. Tidak ada data pasar/foto pengguna disertakan pada fixture repository.
