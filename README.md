# PhiveGarage

Aplikasi pribadi untuk menyeleksi mobil dari katalog lelang PDF. **Java 17 / Spring Boot**, **Vue 3 / Vite**, **PostgreSQL**, OpenAI Responses API.

## Fitur MVP

- Upload katalog PDF teks/scan, diproses asynchronous per 3 halaman (20 MB, maksimal 150 halaman).
- Ekstraksi lot dengan halaman dan kutipan sumber; unknown disimpan null. Katalog asli disimpan di disk server.
- Editor lot: koreksi spesifikasi, surat-surat, biaya aktual, pembanding harga, dan konfirmasi verifikasi.
- Parameter modal, target laba, biaya lelang tetap/persentase, pajak, servis, transport, cadangan risiko, tahun, KM, transmisi, target pembeli, strategi, instruksi tambahan.
- AI menilai demand, likuiditas dan indikasi risiko. Java menghitung biaya, laba, max bid; hasil tersimpan sebagai snapshot.
- Ranking BID / REVIEW / SKIP, detail alasan, bid board, riwayat analisis, re-analysis, export JSON.
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
- `sql/001_schema.sql`: seluruh DDL yang sama dengan migration V1, untuk pemeriksaan/manual setup.

Jika benar-benar memasang DDL secara manual pada DB baru, jalankan script 001 sekali, lalu baseline Flyway versi 1 dengan CLI Flyway sebelum backend pertama kali dinyalakan. Jalur paling sederhana tetap membiarkan aplikasi menjalankan migration V1 sendiri. Jangan menyalakan `baseline-on-migrate` pada database yang belum diperiksa.

## Bagaimana angka dihitung

Semua uang adalah integer IDR. Pembanding adalah harga penawaran yang diinput user (bukan harga transaksi terkonfirmasi). MVP tidak melakukan scraping OLX/Mobil123/Facebook.

- Harga jual konservatif = harga pembanding **terendah** × 95% retail / 85% pedagang. Diskon ini asumsi awal, bukan model pasar terkalibrasi.
- Servis = maksimum(buffer parameter, biaya servis input user, estimasi AI).
- Pajak/balik nama = biaya lot bila diisi, selain itu buffer pajak. Tax expiry belum otomatis menghasilkan tagihan pajak.
- Biaya lain = transport/biaya global + biaya tambahan lot + risk buffer.
- Biaya lelang = biaya tetap + ceil(bid × persen / 100). Isi biaya termasuk pajak/admin sesuai balai; tidak ada tarif balai otomatis.
- Max bid = floor((min(harga jual − target laba, modal tersedia, batas modal per unit) − biaya nonbid − admin tetap) / (1 + persen/100)), minimum 0.
- Profit ditampilkan pada harga dasar katalog, bukan prediksi harga menang.
- Max bid tiap unit menggunakan batas modal yang sama; **bid board tidak mengalokasikan modal portofolio**. Jika mengambil beberapa unit, hitung jumlah modal secara terpisah.

BID membutuhkan verifikasi, pembanding harga, tahun/KM/filter/surat yang terpenuhi, dan base price ≤ max bid. REVIEW berarti bukti belum lengkap. SKIP berarti filter gagal atau harga dasar melewati batas. Skor AI belum terkalibrasi dan tidak menggantikan gate finansial/dokumen.

## AI, biaya, dan batasan

`OPENAI_MODEL` dapat diganti ke model vision yang mendukung PDF dan structured outputs di akun kamu; default `gpt-4.1-mini`. PDF dikirim sebagai base64 input_file ke Responses API, output memakai strict JSON schema. Request menggunakan `store=false`. Isi PDF dan instruksi user diperlakukan sebagai data, bukan instruksi sistem.

Tanpa API key, login dan daftar katalog tetap berfungsi; ekstraksi/analisis membutuhkan key berkuota. Model availability bergantung pada akun. Tidak ada API key atau data katalog nyata di repository.

Penggunaan token yang ditampilkan adalah panggilan analisis yang berhasil; ekstraksi dan panggilan gagal belum diakumulasi. Estimasi USD membutuhkan tarif input/output yang diisi user. Pantau biaya aktual di provider. Antrean satu worker, maksimal 10 pekerjaan menunggu. Pekerjaan yang terhenti saat restart ditandai FAILED; upload ulang atau analisis ulang.

MVP satu pengguna, satu instance backend. Belum SaaS/multi-tenant, belum pengambilan harga pasar otomatis, belum histori transaksi/feedback model. Foto PDF hanya dipakai dalam tahap ekstraksi; belum ada galeri foto atau analisis visual khusus per unit. Tidak dapat memastikan banjir/tabrak/kondisi mesin dari PDF; lakukan inspeksi langsung. Data extraction dapat melewatkan/keliru membaca lot: cocokkan jumlah unit dengan PDF sebelum bidding.

## API

Semua endpoint kecuali health membutuhkan HTTP Basic + header `X-PhiveGarage: web`. Tidak ada CORS lintas origin; frontend dan API memakai origin sama. Password tidak disimpan frontend.

| Endpoint | Fungsi |
|---|---|
| GET /api/health | Liveness |
| GET /api/config | Status AI/model, tanpa key |
| GET /api/catalogs | Daftar katalog |
| POST /api/catalogs | Multipart `file`, `house` |
| GET /api/catalogs/{id} | Status ekstraksi |
| GET /api/catalogs/{id}/pdf | PDF sumber |
| GET /api/catalogs/{id}/lots | Lot terstruktur |
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

Selama development, pengujian dan build dilakukan langsung di server. GitHub digunakan untuk menyimpan source dan riwayat perubahan; tidak ada workflow GitHub Actions. 9 test mencakup budget cap, fee persentase, rounding, deal yang tidak mungkin, serta gate verifikasi/pembanding/risiko servis. Panduan smoke test: `docs/SMOKE_TEST.md`.

API references: [OpenAI PDF](https://developers.openai.com/api/docs/guides/file-inputs), [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs).

## Siklus development di server

1. Ambil perubahan dengan `git pull --ff-only` pada branch `main`.
2. Jalankan test/build Java dan build Vue dengan perintah validasi di atas.
3. Bila build berhasil, restart service backend dan sajikan hasil `frontend/dist` melalui Nginx. Jika memakai Compose, jalankan `docker compose up -d --build`.
4. Periksa login, upload PDF, status ekstraksi, koreksi lot, analisis, max bid, dan bid board mengikuti `docs/SMOKE_TEST.md`.
5. Catat error beserta log backend dan contoh PDF yang memicu masalah untuk diperbaiki.

Untuk instalasi pertama ikuti bagian setup server lebih dahulu. Jangan masukkan `.env`, API key, password, atau katalog pribadi ke git.
