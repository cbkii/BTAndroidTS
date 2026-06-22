#!/usr/bin/env sh
# package-magisk.sh <apk_path> [zip_suffix]
set -eu

APK_PATH="${1:-app/build/outputs/apk/ts18Privileged/release/app-ts18Privileged-release.apk}"
ZIP_SUFFIX="${2:-}"
MODULE_ID="BTAndroidTS"
MODULE_ROOT="magisk"
MODULE_DIR="$MODULE_ROOT/$MODULE_ID"
DEST_DIR="$MODULE_DIR/system/priv-app/BTAndroidTS"
DEST_APK="$DEST_DIR/BTAndroidTS.apk"
BUILD_DIR="build"
ALLOWLIST="$MODULE_DIR/system/etc/permissions/privapp-permissions-com.cbkii.btandroidts.xml"
ORIG_ALLOWLIST_PKG=""
RESTORE_ALLOWLIST=false

cleanup() {
  if [ "$RESTORE_ALLOWLIST" = "true" ] && [ -n "$ORIG_ALLOWLIST_PKG" ] && [ -f "$ALLOWLIST" ]; then
    current_pkg=$(sed -n 's/.*<privapp-permissions package="\([^"]*\)">.*/\1/p' "$ALLOWLIST" | head -n 1 || true)
    if [ -n "$current_pkg" ] && [ "$current_pkg" != "$ORIG_ALLOWLIST_PKG" ]; then
      echo "Restoring allowlist package to $ORIG_ALLOWLIST_PKG"
      sed -i "s/package=\"$current_pkg\"/package=\"$ORIG_ALLOWLIST_PKG\"/" "$ALLOWLIST"
    fi
  fi
}
trap cleanup EXIT HUP INT TERM

echo "--- BTAndroidTS Magisk Packaging ---"
echo "Source APK: $APK_PATH"

if [ ! -f "$APK_PATH" ]; then
  echo "::error::Source APK not found: $APK_PATH" >&2
  exit 1
fi
if [ ! -f "$ALLOWLIST" ]; then
  echo "::error::Privapp allowlist not found: $ALLOWLIST" >&2
  exit 1
fi

rm -f "$DEST_APK"
mkdir -p "$DEST_DIR" "$BUILD_DIR"
cp "$APK_PATH" "$DEST_APK"

AAPT_BIN="aapt"
if ! command -v "$AAPT_BIN" >/dev/null 2>&1; then
  SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/android-sdk}}"
  AAPT_BIN="$(find "$SDK_ROOT/build-tools" -name aapt -type f 2>/dev/null | sort | tail -n 1 || true)"
  if [ -z "$AAPT_BIN" ]; then
    echo "::error::aapt not found in PATH or $SDK_ROOT/build-tools" >&2
    exit 1
  fi
fi

PKG_NAME="$($AAPT_BIN dump badging "$DEST_APK" | sed -n "s/package: name='\([^']*\)'.*/\1/p")"
if [ -z "$PKG_NAME" ]; then
  echo "::error::Failed to extract package name from APK" >&2
  exit 1
fi
echo "Detected package: $PKG_NAME"

IS_DEBUG=false
case "$PKG_NAME" in
  *.debug) IS_DEBUG=true ;;
esac

ORIG_ALLOWLIST_PKG=$(sed -n 's/.*<privapp-permissions package="\([^"]*\)">.*/\1/p' "$ALLOWLIST" | head -n 1 || true)
if [ -z "$ORIG_ALLOWLIST_PKG" ]; then
  echo "::error::Failed to read package name from allowlist: $ALLOWLIST" >&2
  exit 1
fi

if [ "$PKG_NAME" != "$ORIG_ALLOWLIST_PKG" ]; then
  echo "Patching allowlist package from $ORIG_ALLOWLIST_PKG to $PKG_NAME"
  sed -i "s/package=\"$ORIG_ALLOWLIST_PKG\"/package=\"$PKG_NAME\"/" "$ALLOWLIST"
  RESTORE_ALLOWLIST=true
fi

if [ -z "$ZIP_SUFFIX" ]; then
  if [ "$IS_DEBUG" = "true" ]; then
    ZIP_SUFFIX="debug"
  else
    ZIP_SUFFIX="release"
  fi
fi
ZIP_PATH="$BUILD_DIR/BTAndroidTS-$ZIP_SUFFIX-magisk.zip"

sh scripts/validate-magisk-package.sh "$PKG_NAME"

rm -f "$ZIP_PATH"
(
  cd "$MODULE_DIR"
  zip -q -r -9 "../../$ZIP_PATH" .
)

RESTORE_ALLOWLIST=false
cleanup
trap - EXIT HUP INT TERM

echo "ZIP Path: $ZIP_PATH"
echo "Package: $PKG_NAME"
echo "Debug: $IS_DEBUG"
echo "-------------------------------------"
