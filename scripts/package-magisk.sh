#!/usr/bin/env sh
set -eu

APK_PATH="${1:-app/build/outputs/apk/ts18Privileged/release/app-ts18Privileged-release.apk}"
MODULE_DIR="magisk/BTAndroidTS"
DEST_DIR="$MODULE_DIR/system/priv-app/BTAndroidTS"
DEST_APK="$DEST_DIR/BTAndroidTS.apk"
ZIP_PATH="build/BTAndroidTS-magisk.zip"

test -f "$APK_PATH"
mkdir -p "$DEST_DIR" build
cp "$APK_PATH" "$DEST_APK"

if command -v aapt >/dev/null 2>&1; then
  aapt dump badging "$DEST_APK" | grep -q "package: name='com.cbkii.btandroidts'"
else
  AAPT_CANDIDATE="$(find "${ANDROID_HOME:-/opt/android-sdk}" -path '*/build-tools/*/aapt' -type f 2>/dev/null | sort | tail -n 1 || true)"
  if [ -z "$AAPT_CANDIDATE" ]; then
    echo "aapt not found; cannot validate APK package name" >&2
    exit 1
  fi
  "$AAPT_CANDIDATE" dump badging "$DEST_APK" | grep -q "package: name='com.cbkii.btandroidts'"
fi

sh scripts/validate-magisk-package.sh

(
  cd magisk
  zip -qr "../$ZIP_PATH" BTAndroidTS
)

echo "BTAndroidTS Magisk ZIP created at $ZIP_PATH"
