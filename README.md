# Hawa Weather — Android

WebView app + home screen widget untuk [hawa-weather.vercel.app](https://hawa-weather.vercel.app).

- **App**: WebView penuh, tombol back mengikuti riwayat halaman.
- **Widget**: cuaca saat ini per kota, refresh tiap 30 menit (batas minimum Android), ketuk untuk buka app.
- **Data widget**: [Open-Meteo](https://open-meteo.com) langsung — tanpa auth, tanpa bergantung pada deployment Vercel.

## Pasang

1. Unduh `app-debug.apk` dari [release nightly](https://github.com/naufalirfan/hawa-weather-android/releases/download/nightly/app-debug.apk).
2. Izinkan "install from unknown sources" saat diminta.
3. Buka app → web Hawa Weather tampil.

## Pasang widget

1. Tekan lama area kosong di home screen → **Widgets**.
2. Cari **Hawa Weather** → tahan → lepas di home screen.
3. Pilih kota (Jakarta / Yogyakarta / Bandung / Surabaya / Bali).
4. Ketuk widget untuk membuka app.

## Build

Semua compile lewat GitHub Actions (`.github/workflows/android.yml`): unit test + `assembleDebug`, artefak di-upload dan ditempel ke release `nightly`.

```bash
gh run list --repo naufalirfan/hawa-weather-android
gh release download nightly --pattern "*.apk"
```
