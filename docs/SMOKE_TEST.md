# Smoke test server

1. Jalankan PostgreSQL dan backend (Flyway menjalankan migration V1 sampai V4), frontend melalui Vite atau Nginx.
2. `/api/health` menghasilkan UP. `/api/catalogs` tanpa auth harus 401. Dengan auth tanpa X-PhiveGarage harus 403.
3. Login salah ditolak; login benar menampilkan katalog kosong. Pastikan key tidak terdapat pada network response/config atau JS bundle.
4. Upload non-PDF dan PDF >20 MB ditolak. Upload PDF kecil 1–3 halaman dengan 2–3 lot yang bisa diperiksa manual. Status QUEUED → EXTRACTING → READY.
5. Cocokkan jumlah lot, harga, nomor, surat dan sourcePage terhadap PDF; unknown tetap null, tidak fabricated.
6. Analisis tanpa verifikasi/pembanding menghasilkan REVIEW dan max bid kosong. Tidak ada BID berdasarkan estimasi AI saja.
7. Edit satu lot dengan data lengkap/verified dan pembanding. Analisis ulang menghasilkan snapshot baru. Hasil lama tetap tersedia.
8. Contoh: pembanding 80jt, retail => sale 76jt; biaya nonbid 8jt, fixed fee 2jt, target laba 5jt, modal 65jt, fee 0% => max bid 55jt. Base 50jt => total 60jt, laba 16jt. Base 56jt => SKIP.
9. Ubah fee ke 2%; hitung max bid dengan rumus README, pastikan total di max bid tidak melebihi 65jt.
10. Simpan unit ke bid board, refresh katalog/analisis: flag harus tersimpan. Export JSON berisi snapshot parameter/hasil yang dipilih.
11. Hentikan backend di tengah pekerjaan; restart harus menunjukkan FAILED dan tindakan coba ulang ekstraksi/re-analyze dapat dilakukan.
12. Uji tampilan 1440px dan 390px: tabel boleh scroll horizontal, form/sidebar tidak keluar viewport.

Live AI membutuhkan OPENAI_API_KEY milik operator, panggilan berbayar. Tidak ada data pasar/foto pengguna disertakan pada fixture repository.

13. PDF password/rusak harus ditolak dengan pesan yang menjelaskan tindakan. Untuk katalog berstatus FAILED, tombol Coba ulang ekstraksi menggunakan PDF yang sudah tersimpan; retry memanggil AI lagi. Retry pada READY/EXTRACTING harus ditolak 409.
14. Buka detail unit: jumlah servis + pajak + transport/global + biaya lot + risiko + biaya lelang + harga dasar harus sama dengan total modal. Total pada max bid dan profit pada max bid harus sesuai hasil backend.
15. Tahun/KM yang belum terbaca harus menghasilkan REVIEW. Tahun/KM yang diketahui melanggar filter menghasilkan SKIP.

16. Menu Balai lelang: tambah/edit/hapus balai yang belum dipakai, nonaktifkan balai yang sudah dipakai. Nama duplikat ditolak. Upload hanya memilih balai aktif.
17. JBA pada bid 50 juta: admin 3 juta, pajak 550 ribu, ALL IN lelang 53,55 juta. Bandingkan preview backend dan kolom lot.
18. Edit master tarif: katalog lama harus tetap memakai snapshot lama sampai tombol Terapkan tarif diklik. Riwayat analisis harus tetap memakai biaya lama.
19. Input maksimum bid di bawah harga dasar: unit SKIP tanpa AI. STNK UNKNOWN/TIDAK_ADA/null juga SKIP tanpa AI, meskipun client mencoba menonaktifkan requireStnk. Lihat jumlah aiAnalyzedLots/filteredLots pada usage hasil.


## Unit dimenangkan
1. Pilih Catat menang pada lot katalog, isi harga aktual, simpan. Ulangi lot yang sama: harus konflik, tidak ada duplikasi.
2. Tambahkan unit manual dari balai lain. Ubah master tarif setelah pencatatan: tarif unit lama harus tetap sama.
3. Contoh sintetis JBA: bid aktual Rp50 juta, bid ideal Rp48 juta, biaya Rp2 juta, target Rp65 juta. Pajak Rp550 ribu; modal real Rp53,55 juta; ALL IN akhir Rp55,55 juta; margin harapan Rp9,45 juta; modal ideal Rp51,528 juta.
4. Edit/hapus pengeluaran dan periksa seluruh total. Nominal nol/negatif harus ditolak.
5. Isi deal Rp62 juta dan tanggal terjual: margin real Rp6,45 juta. Harga deal tanpa tanggal, atau tanggal sebelum menang, harus ditolak.
6. Dana awal Rp57 juta: Dana menjadi Rp63,45 juta bila hanya ada penjualan tersebut. Unit belum terjual menambah modal tersimpan, bukan margin real. Uji penjualan rugi juga mengurangi Dana.
7. Restart backend dan pastikan data/biaya/dana tetap tersimpan. Uji autentikasi endpoint baru, pengeluaran milik unit lain tidak boleh diedit lewat ID unit berbeda.
8. Hapus unit: rincian biayanya ikut terhapus. Balai/lot yang masih dirujuk unit tidak dapat dihapus.


## Keputusan beli
1. Tanpa API key, hitung unit seller langsung dengan data sintetis: ask Rp48 juta, modal Rp65 juta, batas user Rp50 juta, target Rp5 juta, servis Rp2–5 juta, biaya lain/pajak/cadangan nol, harga jual Rp50/53/55 juta. Tandai STNK/BPKB ADA, bukti harga terisi, harga dan inspeksi terverifikasi. Harus Nego dulu, batas Rp40 juta, buka Rp36–38 juta; ALL IN Rp50–53 juta, titik impas Rp53 juta, target jual Rp58 juta; laba konservatif pada penawaran −Rp3 juta.
2. Hapus verifikasi: Periksa dulu. STNK UNKNOWN atau risiko berat: Lewati dan tombol AI tidak aktif. Batas user Rp30 juta menurunkan batas beli; biaya tinggi/pajak/cadangan lebih besar menurunkan batas.
3. Input rentang tidak berurutan atau centang harga terverifikasi tanpa rincian pembanding: harus ditolak backend. JPG/PNG valid diterima, file palsu/lebih dari 6/lebih dari 3 MB/foto terlalu besar ditolak.
4. Dengan API key, analisis AI dan foto: hasil harus menjelaskan indikasi, risiko dan belum terverifikasi; perhitungan/batas beli tetap sama. Pastikan token muncul, kegagalan bisa retry tanpa kehilangan hitungan, request bersamaan tidak menduplikasi proses, restart tidak meninggalkan status PROCESSING.
5. Foto tidak boleh diakses tanpa autentikasi atau lewat decision ID lain. Backup folder data bersama database.
6. Buat keputusan dari katalog: identitas, jenis AUCTION, sumber dan tarif mengikuti katalog. Ubah tarif master setelah laporan dibuat: laporan lama tetap sama.
7. Catat sudah dibeli dengan harga aktual: tampil di inventory, seller langsung admin/pajak lelang nol. Tidak boleh duplikat pembelian dari satu laporan/lot. Edit inventory seller langsung dan periksa tidak membutuhkan balai. Estimasi servis bukan pengeluaran aktual.
8. Ubah input / buat simulasi baru: hasil sebelumnya tetap tersedia di riwayat. Uji tampilan mobile dan seluruh tabel dapat digeser.
