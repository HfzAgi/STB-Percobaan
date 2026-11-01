# Beranda — launcher STB Android TV yang ringan

Launcher sederhana dan bersih untuk STB Android TV (dibuat untuk ZTE B866F Android 14, tapi bisa dipakai di Android TV 8.0 ke atas).

**Isi layar utama**
- Jam besar, tanggal, dan tanggal Hijriah + hitung mundur Ramadan / Idul Fitri / Idul Adha
- Cuaca sekarang (Open-Meteo, gratis tanpa API key), diperbarui tiap 30 menit saja
- Jadwal sholat (Imsak–Isya) dihitung langsung di STB dengan kriteria Kemenag RI, tanpa internet
- Kartu musik yang sedang diputar (Spotify, YouTube Music, dll) dengan tombol putar/jeda/lanjut
- Deretan aplikasi favorit + tombol *Semua aplikasi* dan *Pengaturan*
- Wallpaper gradasi yang berubah sesuai jam dan cuaca (tanpa foto berat)
- Screensaver jam

**Kenapa ringan:** tanpa library tambahan (APK hanya puluhan KB), tidak ada layanan yang jalan di latar, semua pembaruan berhenti saat kamu sedang di aplikasi lain.

## Download

Buka tab **Releases** di repo ini lalu download `Beranda.apk`, atau langsung:

```
https://github.com/<username>/stb-launcher/releases/latest/download/Beranda.apk
```

Setiap ada perubahan kode, GitHub otomatis membuat APK baru (lihat tab **Actions**).

## Cara pasang ke STB (tanpa laptop)

1. Pasang aplikasi **Send Files to TV** di HP dan di STB (ada di Play Store), lalu kirim `Beranda.apk` dari HP ke STB.
   Atau: salin APK ke flashdisk (pakai OTG di HP), lalu colok ke STB.
2. Di STB, buka file APK lewat file manager dan pasang. Kalau diminta, izinkan pemasangan dari sumber tidak dikenal.
3. Buka **Beranda** → **Pengaturan**:
   - **Jadikan launcher utama** supaya tombol Home membuka Beranda.
   - **Izin widget musik** supaya kartu musik muncul (pilih *Otomatis (root)*, lalu setujui permintaan root).
   - **Screensaver jam** → *Pasang otomatis (root)*.
   - **Kota** untuk cuaca & jadwal sholat.

## Cara pakai

- **OK**: buka aplikasi.
- **Tahan OK** (atau tombol **Menu** di remote) pada aplikasi: geser posisi, hapus dari beranda, info aplikasi, uninstall.
- Di **Semua aplikasi**, tahan OK untuk menambah aplikasi ke beranda.

## Update

Pasang APK versi baru menimpa yang lama, tanpa perlu uninstall. Pengaturan tetap tersimpan.
