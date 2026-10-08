# PhiveGarage

Aplikasi pribadi untuk menyeleksi mobil dari katalog lelang PDF. **Java 17 / Spring Boot**, **Vue 3 / Vite**, **PostgreSQL**, OpenAI Responses API.

## Fitur MVP

- Upload katalog PDF teks/scan, diproses asynchronous per 3 halaman (20 MB, maksimal 150 halaman).
- Coba ulang ekstraksi katalog gagal tanpa upload ulang; PDF rusak/password memberikan pesan tindakan.
- Ekstraksi lot dengan halaman dan kutipan sumber; unknown disimpan null. Katalog asli disimpan di disk server.
- Editor lot: koreksi spesifikasi, surat-surat, biaya aktual, pembanding harga, dan konfirmasi verifikasi.
- Master balai lelang CRUD: admin tetap + pajak dari harga bid. Tarif JBA awal 3 juta + 1,1% dari bid (input user); balai lainnya diisi manual.
- Parameter maksimum harga bid, modal, target laba, biaya lelang tetap/persentase, pajak, servis, transport, cadangan risiko, tahun, KM, transmisi, target pembeli, strategi, instruksi tambahan.
- Backend menyaring unit sebelum AI: wajib STNK ADA, harga dasar ≤ batas bid, filter dasar dan modal minimum terpenuhi. Unit tersaring tetap dicatat dengan alasan dan tidak memakai token analisis.
- AI menilai demand, likuiditas dan indikasi risiko. Java menghitung biaya, laba, max bid; hasil tersimpan sebagai snapshot.
- Ranking BID / REVIEW / SKIP, rincian biaya dan profit pada harga dasar/max bid, detail alasan, bid board, riwayat analisis, re-analysis, export JSON.
- Login satu akun private workspace; API key hanya di server. Dashboard penggunaan token untuk analisis terpilih.

## Jalankan di server tanpa Docker

Prasyarat: JDK 17+, Maven 3.9+, Node.js 22+, PostgreSQL 15+, Nginx.

```bash
git clone https://github.com/philipwanderrienov/phivegarage.git
cd phivegarage
cp .env.example .env
# Edit .env: DATABASE_PASSWORD, APP_PASSWORD (12+ karakter), OPENAI_API_KEY
psql -U postgres -f sql/000_database.sql
# Ganti password placeholder pada script sebelum menjalankannya.
mvn -f backend/pom.xml clean verify
cd frontend
npm install
npm run build
cd ..
```

Backend menggunakan Flyway untuk membuat tabel otomatis. Jangan menjalankan `001_schema.sql` lagi pada DB yang dikelola Flyway. `000_database.sql` membutuhkan administrator dan hanya dijalankan sekali.

Untuk development, dengan PostgreSQL aktif:

```bash
set -a
. ./.env
set +a
java -jar backend/target/phivegarage-0.1.0.jar
# Terminal lain:
cd frontend
npm run dev
```

Buka `http://localhost:5173`. Vite meneruskan `/api` ke backend port 8080. Login menggunakan APP_USERNAME/APP_PASSWORD.

Untuk service Linux:

1. Simpan project di `/opt/phivegarage`, buat OS user `phivegarage`, beri user tersebut hak baca project dan tulis direktori `data`. `.env` harus hanya bisa dibaca pemilik service (`chmod 600`).
2. Atur `STORAGE_PATH=/opt/phivegarage/data` dan `DATABASE_URL` pada `.env`.
3. Salin `deploy/phivegarage.service` ke `/etc/systemd/system/`, lalu `systemctl daemon-reload` dan `systemctl enable --now phivegarage`.
4. Gunakan `deploy/nginx-native.conf`; ubah domain/root, konfigurasi HTTPS sebelum akses publik. Akses backend port 8080 hanya dari reverse proxy (firewall).
5. Backup PostgreSQL dan folder PDF `data` bersama. Semua lot/analisis disimpan di PostgreSQL, PDF di disk.

## Alternatif Docker Compose

```bash
cp .env.example .env
# Isi password dan OpenAI API key.
docker compose up -d --build
```

Akses lokal: `http://localhost:8090`; Compose bind ke localhost. Untuk akses publik gunakan reverse proxy HTTPS ke port ini. DB/API tidak dipublikasikan oleh Compose. Skema otomatis oleh Flyway, tidak perlu menjalankan SQL manual. Jangan `docker compose down -v` jika ingin mempertahankan data.

## Script SQL manual

- `sql/000_database.sql`: role/database, dijalankan sebagai admin. Ganti password placeholder.
- `sql/001_schema.sql`: DDL awal yang sama dengan migration V1.
- `sql/004_purchase_decisions.sql`: migration V4, laporan keputusan beli, foto unit, dan hubungan pembelian aktual ke inventory.
- `sql/003_won_units.sql`: migration V3, unit dimenangkan, pengeluaran per unit, serta dana awal.
- `sql/002_auction_houses.sql`: migration V2, tabel balai lelang + referensi dan snapshot tarif katalog. Flyway menjalankannya otomatis, termasuk untuk instalasi V1 yang sudah ada.

Jika benar-benar memasang DDL secara manual pada DB baru, jalankan script 001, 002, 003, lalu 004 sekali sesuai urutan, lalu baseline Flyway versi 4 dengan CLI Flyway sebelum backend pertama kali dinyalakan. Jalur paling sederhana tetap membiarkan aplikasi menjalankan migration V1 sendiri. Jangan menyalakan `baseline-on-migrate` pada database yang belum diperiksa.

## Bagaimana angka dihitung

Semua uang adalah integer IDR. Pembanding adalah harga penawaran yang diinput user (bukan harga transaksi terkonfirmasi). MVP tidak melakukan scraping OLX/Mobil123/Facebook.

- Harga jual konservatif = harga pembanding **terendah** × 95% retail / 85% pedagang. Diskon ini asumsi awal, bukan model pasar terkalibrasi.
- Servis = maksimum(buffer parameter, biaya servis input user, estimasi AI).
- Pajak/balik nama = biaya lot bila diisi, selain itu buffer pajak. Tax expiry belum otomatis menghasilkan tagihan pajak.
- Biaya lain = transport/biaya global + biaya tambahan lot + risk buffer.
- Biaya lelang = biaya tetap + ceil(bid × persen / 100). Admin tetap dan persentase pajak berasal dari snapshot balai yang dipilih. Pajak lelang dihitung dari bid, berbeda dari tunggakan pajak kendaraan.
- Max bid = floor((min(harga jual − target laba, modal tersedia, batas modal per unit) − biaya nonbid − admin tetap) / (1 + persen/100)), minimum 0, lalu dibatasi maksimum harga bid input user.
- Profit ditampilkan pada harga dasar katalog, bukan prediksi harga menang.
- Max bid tiap unit menggunakan batas modal yang sama; **bid board tidak mengalokasikan modal portofolio**. Jika mengambil beberapa unit, hitung jumlah modal secara terpisah.

BID membutuhkan verifikasi, STNK ADA, pembanding harga, tahun/KM/filter/surat yang terpenuhi, dan base price ≤ max bid. REVIEW berarti bukti belum lengkap. SKIP berarti filter gagal atau harga dasar melewati batas. Skor AI belum terkalibrasi dan tidak menggantikan gate finansial/dokumen.

## AI, biaya, dan batasan

`OPENAI_MODEL` dapat diganti ke model vision yang mendukung PDF dan structured outputs di akun kamu; default `gpt-4.1-mini`. PDF dikirim sebagai base64 input_file ke Responses API, output memakai strict JSON schema. Request menggunakan `store=false`. Isi PDF dan instruksi user diperlakukan sebagai data, bukan instruksi sistem.

Tanpa API key, login dan daftar katalog tetap berfungsi; ekstraksi/analisis membutuhkan key berkuota. Model availability bergantung pada akun. Tidak ada API key atau data katalog nyata di repository.

Penggunaan token yang ditampilkan adalah panggilan analisis yang berhasil; ekstraksi dan panggilan gagal belum diakumulasi. Estimasi USD membutuhkan tarif input/output yang diisi user. Pantau biaya aktual di provider. Antrean satu worker, maksimal 10 pekerjaan menunggu. Pekerjaan yang terhenti saat restart ditandai FAILED; coba ulang ekstraksi dari katalog tersimpan atau analisis ulang.

MVP satu pengguna, satu instance backend. Belum SaaS/multi-tenant, belum pengambilan harga pasar otomatis, belum histori transaksi/feedback model. Foto PDF hanya dipakai dalam tahap ekstraksi; belum ada galeri foto atau analisis visual khusus per unit. Tidak dapat memastikan banjir/tabrak/kondisi mesin dari PDF; lakukan inspeksi langsung. Data extraction dapat melewatkan/keliru membaca lot: cocokkan jumlah unit dengan PDF sebelum bidding.

## API

Semua endpoint kecuali health membutuhkan HTTP Basic + header `X-PhiveGarage: web`. Tidak ada CORS lintas origin; frontend dan API memakai origin sama. Password tidak disimpan frontend.

| Endpoint | Fungsi |
|---|---|
| GET /api/health | Liveness |
| GET /api/config | Status AI/model, tanpa key |
| GET /api/catalogs | Daftar katalog |
| POST /api/catalogs | Multipart `file`, `houseId` |
| GET /api/catalogs/{id} | Status ekstraksi |
| POST /api/catalogs/{id}/retry | Coba ulang katalog FAILED (menggunakan kuota AI lagi) |
| GET /api/catalogs/{id}/pdf | PDF sumber |
| GET /api/catalogs/{id}/lots | Lot terstruktur dan ALL IN lelang pada harga dasar |
| PUT /api/catalogs/{id}/house | Pilih/perbarui snapshot balai `{houseId}` |
| POST /api/catalogs/{id}/cost-preview | Preview ALL IN `{bid}` tanpa AI |
| GET/POST /api/auction-houses | Daftar / tambah balai |
| PUT/DELETE /api/auction-houses/{id} | Edit / hapus balai |
| PUT /api/lots/{id} | `{data, verified}` |
| POST /api/catalogs/{id}/analyses | Parameters JSON |
| GET /api/catalogs/{id}/analyses | Riwayat |
| GET /api/analyses/{id} | Hasil/status |
| PATCH /api/analyses/{id}/watchlist/{lotId} | `{watchlisted: true}` |

## Validasi

```bash
mvn -f backend/pom.xml verify
cd frontend && npm run build
```

Selama server maintenance, pengujian dan build dilakukan secara lokal. Setelah server siap, pengujian integrasi dilakukan langsung di server. GitHub digunakan untuk menyimpan source dan riwayat perubahan; tidak ada workflow GitHub Actions. 52 test mencakup budget cap, fee persentase, rounding, deal yang tidak mungkin, gate verifikasi/pembanding/risiko servis, validasi structured output AI, PDF valid/rusak/password, dan rekonsiliasi rincian biaya, tarif JBA, batas bid user dan prefilter STNK/modal. Panduan smoke test: `docs/SMOKE_TEST.md`.

API references: [OpenAI PDF](https://developers.openai.com/api/docs/guides/file-inputs), [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs).

## Siklus development di server

1. Ambil perubahan dengan `git pull --ff-only` pada branch `main`.
2. Jalankan test/build Java dan build Vue dengan perintah validasi di atas.
3. Bila build berhasil, restart service backend dan sajikan hasil `frontend/dist` melalui Nginx. Jika memakai Compose, jalankan `docker compose up -d --build`.
4. Periksa login, upload PDF, status ekstraksi, koreksi lot, analisis, max bid, dan bid board mengikuti `docs/SMOKE_TEST.md`.
5. Catat error beserta log backend dan contoh PDF yang memicu masalah untuk diperbaiki.

Untuk instalasi pertama ikuti bagian setup server lebih dahulu. Jangan masukkan `.env`, API key, password, atau katalog pribadi ke git.

## Tarif balai dan ALL IN

Tarif katalog disimpan sebagai snapshot saat upload, sehingga edit master balai tidak mengubah katalog lama. Tombol **Terapkan tarif** mengambil konfigurasi master terbaru untuk katalog tersebut; hasil analisis lama tetap memakai snapshot parameternya. Balai yang sudah dipakai katalog tidak dapat dihapus: nonaktifkan agar tidak tersedia untuk upload baru.

ALL IN lelang = bid + admin tetap + ceil(bid × pajak lelang / 100). Contoh JBA bid 50 juta => admin 3 juta + pajak 550 ribu => ALL IN lelang 53,55 juta. ALL IN lelang belum termasuk servis, pajak kendaraan/balik nama, transport dan cadangan risiko; biaya tersebut masuk **total modal usaha**.

Katalog legacy JBA dihubungkan ke seed tarif JBA pada migration V2. Katalog legacy balai lain tetap tanpa tarif sampai user memilih balai yang sudah dikonfigurasi. Tidak ada tarif balai lain yang ditebak. Field `maxBid` wajib diisi untuk analisis baru; fee input client tidak dapat mengganti tarif snapshot katalog, dan STNK ADA tetap wajib meskipun client mengirim requireStnk=false.


## Unit dimenangkan (Board & Simulation)

Menu **Unit dimenangkan** mengikuti kolom dan rumus spreadsheet referensi PHIVE Garage. Tambahkan unit manual dengan memilih balai, atau klik **Catat menang** pada lot katalog; isi harga menang aktual. Tidak ada impor otomatis data pribadi dari spreadsheet.

| Perhitungan | Rumus |
| --- | --- |
| Modal maksimal / menang ideal | Bid ideal + admin + pajak bid ideal |
| Modal real / ALL IN lelang | Harga menang aktual + admin + pajak harga menang aktual |
| Perbaikan + penggunaan | Jumlah seluruh baris pengeluaran unit |
| Harga akhir ALL IN | Modal real + perbaikan/penggunaan |
| Margin harapan | Harga iklan / target jual − harga akhir ALL IN |
| Margin real | Harga deal jual − harga akhir ALL IN |
| Dana sesuai rumus sheet | Dana awal + akumulasi margin real seluruh unit terjual |

Pajak lelang dibulatkan ke atas ke rupiah penuh, sama dengan perhitungan bid. Median pasar dicatat sebagai referensi; target dan deal merupakan input user. Harga deal dan tanggal terjual wajib diisi bersama; margin real kosong sebelum terjual. Laba negatif tetap dicatat sebagai kerugian.

Dana bukan saldo kas bank: modal unit belum terjual ditampilkan terpisah dan tidak dikurangkan dari rumus Dana. Isi dana awal pada menu ini. Dana awal berlaku untuk seluruh garasi; setiap unit terjual menyumbang margin realnya satu kali berdasarkan record unit, bukan setiap kali record diedit.

Pengeluaran mendukung tambah, edit, hapus: tanggal, kategori (bebas, tersedia saran servis/BBM/dokumen/tol/cuci/iklan), catatan, nominal rupiah positif. ALL IN dan margin dihitung ulang dari rincian aktual. Hapus unit juga menghapus pengeluarannya.

Tarif balai dibekukan pada unit. Unit dari PDF memakai snapshot tarif katalog, unit manual memakai tarif aktif saat pencatatan. Balai dan sumber tidak dapat diubah setelah disimpan. Satu lot katalog hanya boleh dicatat menang sekali. Balai dan lot yang dirujuk unit dimenangkan tidak dapat dihapus. Perhitungan ini berjalan di Java dan tidak memanggil AI.

API: `/api/won-units` (GET/POST), `/{id}` (GET/PUT/DELETE), `/{id}/expenses` (POST), `/{id}/expenses/{expenseId}` (PUT/DELETE), `/funds` (GET/PUT). Seluruh endpoint memakai autentikasi aplikasi yang sama.


## Keputusan beli (seller langsung & lelang)

Menu **Keputusan beli** menghasilkan laporan yang bisa dipakai untuk nego, menetapkan batas beli, atau melewatkan unit. Data dapat diisi manual atau diawali lewat tombol Keputusan beli pada lot katalog. Untuk seller langsung, biaya balai nol; untuk lelang, tarif master dibekukan saat laporan dibuat, atau memakai snapshot katalog untuk lot sumber.

Input: identitas unit, STNK/BPKB, harga penawaran, modal per unit, batas beli user, target laba, servis rendah/tinggi, pajak kendaraan/dokumen, biaya lain, cadangan risiko, harga jual rendah/tengah/tinggi, bukti pembanding, verifikasi harga/inspeksi, serta risiko berat. Harga jual merupakan input user, tidak dicari otomatis. Centang harga terverifikasi mensyaratkan rincian pembanding terisi.

- Batas beli = min(batas user, batas dari harga jual rendah − target laba, modal) setelah memperhitungkan biaya servis tinggi, biaya lain, cadangan, admin, dan pajak lelang. Pajak dibulatkan ke atas; batas beli dibulatkan ke bawah.
- Buka nego = 90–95% batas beli, dibulatkan ke bawah Rp500 ribu untuk nominal di atas Rp1 juta. Ini heuristik yang ditampilkan eksplisit, bukan hasil survei harga atau jaminan seller menerima.
- Titik impas = ALL IN pada harga penawaran dengan biaya tinggi. Harga jual untuk target laba = titik impas + target laba.
- Simulasi menampilkan penawaran, batas beli, dua harga buka nego: ALL IN rendah/tinggi, laba pada tiga harga jual menggunakan biaya rendah, dan laba konservatif menggunakan harga jual rendah serta biaya tinggi.
- Keputusan: Lewati jika STNK bukan ADA, risiko berat dicentang, atau tidak ada bid/beli positif yang memenuhi hitungan. Periksa dulu jika harga pasar/inspeksi/BPKB belum diverifikasi. Nego dulu jika penawaran melebihi batas. Beli bersyarat jika input terverifikasi dan harga memenuhi hitungan. Verifikasi merupakan pernyataan user, bukan pemeriksaan independen aplikasi.

**AI & foto bersifat opsional.** Tombol Hitung & simpan bekerja tanpa API key dan tidak memakai AI. Foto JPG/PNG (maksimal 6 × 3 MB, 40 megapixel) disimpan privat di folder data server. Analisis AI dijalankan terpisah dengan tombol; memanggil OpenAI API berbayar dan mengirim input serta foto unit. AI memberikan ringkasan, indikasi likuiditas/target pembeli, strategi nego, indikasi visual, risiko, checklist, dan hal belum diketahui; tidak mengganti perhitungan Java. Tidak ada akses web, riset transaksi langsung, jaminan waktu jual, diagnosis mesin, atau konfirmasi bebas banjir/tabrakan dari foto. Unit berstatus Lewati (STNK bukan ADA, risiko berat, atau tidak ada harga beli layak) tidak dikirim ke AI. Retry memakai kuota kembali; token model/input/output ditampilkan pada laporan. Gunakan AI & penggunaan untuk tarif model yang diisi user; estimasi biaya dashboard saat ini masih untuk analisis katalog, bukan gabungan laporan keputusan.

Laporan adalah snapshot tersimpan; tombol ubah input membuat laporan baru (verifikasi harga/inspeksi kembali tidak dicentang) sehingga hasil lama tetap tersedia. Kegagalan AI tidak menghapus hitungan. Job AI yang terputus saat restart ditandai gagal dan bisa dicoba ulang. Foto diakses melalui endpoint berautentikasi, bukan URL publik.

Setelah transaksi selesai, **Catat sudah dibeli** meminta harga dan tanggal aktual, lalu membuat record di Unit dimenangkan menggunakan tarif laporan. Seller langsung tidak diwajibkan memiliki balai. Estimasi servis tidak otomatis menjadi pengeluaran aktual; input biaya aktual pada inventory. Satu laporan dan satu lot katalog tidak dapat dicatat sebagai pembelian dua kali. Catatan inventory dapat dikoreksi dengan tarif/sumber tetap terkunci.

API: `/api/purchase-decisions` GET/POST (multipart `input` JSON + `photos`), `/{id}` GET, `/{id}/ai` POST, `/{id}/acquire` POST, `/{id}/photos/{photoId}` GET. Endpoint baru mengikuti Basic Auth + X-PhiveGarage yang sama.
