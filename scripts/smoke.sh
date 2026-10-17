#!/usr/bin/env bash
# Smoke test di emulator. Dijalankan sebagai satu perintah karena
# android-emulator-runner mengeksekusi setiap baris script secara terpisah.
set -euo pipefail

APK=app/build/outputs/apk/debug/app-debug.apk
PKG=com.naufal.hawaweather
URL=https://hawa-weather.vercel.app

adb logcat -c
adb install -r "$APK"
adb shell am start -W -n "$PKG/.MainActivity"

for _ in $(seq 1 60); do
  adb logcat -d | grep -q "HawaWeb.*PAGE_FINISHED" && break
  sleep 2
done

adb logcat -d > /tmp/logcat.txt

echo "--- page load ---"
grep "HawaWeb" /tmp/logcat.txt | tail -20 || true

grep -q "HawaWeb.*PAGE_FINISHED $URL" /tmp/logcat.txt || { echo "PAGE NOT LOADED"; exit 1; }
if grep -q "HawaWeb.*PAGE_ERROR" /tmp/logcat.txt; then echo "PAGE ERROR"; exit 1; fi

echo "--- widget provider ---"
adb shell dumpsys appwidget | grep -q "cmp:ComponentInfo{$PKG/$PKG.HawaWidget" \
  || { echo "WIDGET PROVIDER NOT REGISTERED"; exit 1; }

echo "--- crashes ---"
if grep -E "FATAL EXCEPTION|AndroidRuntime: E " /tmp/logcat.txt; then echo "CRASH DETECTED"; exit 1; fi

echo "--- resumed ---"
adb shell dumpsys activity activities | grep -q "topResumedActivity.*$PKG/.MainActivity" \
  || { echo "APP NOT IN FOREGROUND"; exit 1; }

adb exec-out screencap -p > /tmp/screen.png
echo SMOKE_OK
