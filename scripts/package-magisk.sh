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

# Attempt to find aapt robustly
AAPT_BIN="aapt"
if ! command -v "$AAPT_BIN" >/dev/null 2>&1; then
  # Try to find it in ANDROID_HOME or ANDROID_SDK_ROOT
  SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/android-sdk}}"
  AAPT_BIN="$(find "$SDK_ROOT/build-tools" -name aapt -type f 2>/dev/null | sort | tail -n 1 || true)"
  if [ -z "$AAPT_BIN" ]; then
    echo "::error::aapt not found in PATH or $SDK_ROOT/build-tools" >&2
    exit 1
  fi
fi

"$AAPT_BIN" dump badging "$DEST_APK" | grep -q "package: name='com.cbkii.btandroidts'"

sh scripts/validate-magisk-package.sh

(
  cd magisk
  zip -qr "../$ZIP_PATH" BTAndroidTS
)

echo "BTAndroidTS Magisk ZIP created at $ZIP_PATH"
