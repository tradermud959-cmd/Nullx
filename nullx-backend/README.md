# NullX AI Backend untuk Termux

Ini adalah backend native Termux (berbasis Node.js + Express) untuk menghubungkan aplikasi Android NullX AI ke server Ollama lokal.

## Persyaratan
- Aplikasi Termux di Android.
- Ollama sudah terinstall dan berjalan di Termux (menggunakan distro linux di Termux atau termux-native jika tersedia).

## Cara Instalasi
1. Pindahkan folder `nullx-backend` ke dalam penyimpanan Termux Anda.
2. Buka aplikasi Termux dan masuk ke folder proyek:
   ```bash
   cd nullx-backend
   ```
3. Jalankan script instalasi:
   ```bash
   ./install.sh
   ```
   Script ini akan mengupdate package Termux, menginstall Node.js, curl, dan dependensi lainnya.

## Cara Menjalankan
Jalankan script start:
```bash
./start.sh
```
Script ini akan:
1. Memeriksa apakah Ollama sudah berjalan (jika belum akan otomatis dijalankan di background).
2. Menentukan IP lokal dari perangkat Android (misal: 192.168.1.x).
3. Menjalankan backend server dan mencetak IP:Port di terminal.

## Cara Menghentikan Backend
Tekan `CTRL + C` di terminal Termux Anda.

## Konfigurasi Lanjutan
- **Ganti Model:** Buka file `start.sh` lalu ubah `export MODEL="llama3.2:1b"` ke model lain, atau dari frontend di Android nanti (bisa diatur).
- **Ganti Port Backend:** Buka file `start.sh` lalu ubah `export PORT=5000`.
- **Debugging:** Buka aplikasi Android dan pantau langsung dari **Log Server** di halaman pengaturan, atau pantau terminal di Termux.

## Struktur Proyek
- `install.sh`: Script instalasi otomatis.
- `start.sh`: Script startup untuk mengecek Ollama & menjalankan backend.
- `package.json`: Definisi dependensi Node.js.
- `src/server.js`: File utama backend.
- `src/config/`: Konfigurasi global (port, URL Ollama).
- `src/controllers/`: Logika endpoint chat dan status.
- `src/routes/`: Definisi routing API.
- `src/utils/logger.js`: Sistem logging untuk dikirim ke Android.
