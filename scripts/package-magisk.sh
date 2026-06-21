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

echo "--- BTAndroidTS Magisk Packaging ---"
echo "Source APK: $APK_PATH"

if [ ! -f "$APK_PATH" ]; then
  echo "::error::Source APK not found: $APK_PATH" >&2
  exit 1
fi

# Clean up stale files in module dir and build dir
rm -f "$DEST_APK"
mkdir -p "$DEST_DIR" "$BUILD_DIR"

# Copy APK
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

# Extract package name from APK
PKG_NAME="$("$AAPT_BIN" dump badging "$DEST_APK" | sed -n "s/package: name='\([^']*\)'.*/\1/p")"
if [ -z "$PKG_NAME" ]; then
  echo "::error::Failed to extract package name from APK" >&2
  exit 1
fi
echo "Detected package: $PKG_NAME"

# Determine if this is a debug build
IS_DEBUG=false
case "$PKG_NAME" in
  *.debug) IS_DEBUG=true ;;
esac

# Update allowlist package name temporarily if it's a debug build
ORIG_ALLOWLIST_PKG=$(sed -n 's/.*<privapp-permissions package="\([^"]*\)">.*/\1/p' "$ALLOWLIST")
if [ "$PKG_NAME" != "$ORIG_ALLOWLIST_PKG" ]; then
  echo "Patching allowlist package from $ORIG_ALLOWLIST_PKG to $PKG_NAME"
  sed -i "s/package=\"$ORIG_ALLOWLIST_PKG\"/package=\"$PKG_NAME\"/" "$ALLOWLIST"
fi

# Construct ZIP path
if [ -z "$ZIP_SUFFIX" ]; then
  if $IS_DEBUG; then
    ZIP_SUFFIX="debug"
  else
    ZIP_SUFFIX="release"
  fi
fi
ZIP_PATH="$BUILD_DIR/BTAndroidTS-$ZIP_SUFFIX-magisk.zip"

# Run validation before zipping
# We pass the package name to the validation script
sh scripts/validate-magisk-package.sh "$PKG_NAME"

# Create Magisk module ZIP
# Magisk requires the module files (module.prop, etc.) at the root of the ZIP.
rm -f "$ZIP_PATH"
(
  cd "$MODULE_DIR"
  # -q: quiet, -r: recursive, -9: better compression
  zip -q -r -9 "../../$ZIP_PATH" .
)

# Restore allowlist if it was changed
if [ "$PKG_NAME" != "$ORIG_ALLOWLIST_PKG" ]; then
  echo "Restoring allowlist package to $ORIG_ALLOWLIST_PKG"
  sed -i "s/package=\"$PKG_NAME\"/package=\"$ORIG_ALLOWLIST_PKG\"/" "$ALLOWLIST"
fi

echo "ZIP Path: $ZIP_PATH"
echo "Package: $PKG_NAME"
echo "Debug: $IS_DEBUG"
echo "-------------------------------------"
