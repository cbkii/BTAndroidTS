#!/usr/bin/env sh
# validate-magisk-package.sh [expected_package_name]
set -eu

EXPECTED_PKG="${1:-com.cbkii.btandroidts}"
MODULE_DIR="magisk/BTAndroidTS"
ALLOWLIST="$MODULE_DIR/system/etc/permissions/privapp-permissions-com.cbkii.btandroidts.xml"
APK="$MODULE_DIR/system/priv-app/BTAndroidTS/BTAndroidTS.apk"
PROP_FILE="$MODULE_DIR/module.prop"

SCRIPT_FILES="$MODULE_DIR/post-fs-data.sh $MODULE_DIR/service.sh $MODULE_DIR/customize.sh"
TEXT_FILES="$PROP_FILE $ALLOWLIST $SCRIPT_FILES"

echo "Validating Magisk module at $MODULE_DIR"

for f in $TEXT_FILES "$APK"; do
  if [ ! -f "$f" ]; then
    echo "::error::Missing file: $f" >&2
    exit 1
  fi
done

CR="$(printf '\r')"
if grep -Iq "$CR" $TEXT_FILES; then
  echo "::error::CRLF line endings detected in module files. Only LF is allowed." >&2
  exit 1
fi

if ! grep -q "^id=btandroidts$" "$PROP_FILE"; then
  echo "::error::module.prop: id must be 'btandroidts'" >&2
  exit 1
fi
if ! grep -q "^name=" "$PROP_FILE"; then
  echo "::error::module.prop: missing name" >&2
  exit 1
fi
if ! grep -q "^version=" "$PROP_FILE"; then
  echo "::error::module.prop: missing version" >&2
  exit 1
fi
if ! grep -q "^versionCode=[0-9]\+$" "$PROP_FILE"; then
  echo "::error::module.prop: invalid or missing versionCode" >&2
  exit 1
fi

if ! grep -q "package=\"$EXPECTED_PKG\"" "$ALLOWLIST"; then
  echo "::error::Allowlist package mismatch. Expected: $EXPECTED_PKG" >&2
  grep "package=" "$ALLOWLIST" || true
  exit 1
fi

if ! grep -q 'android.permission.BLUETOOTH_PRIVILEGED' "$ALLOWLIST"; then
  echo "::error::Allowlist missing BLUETOOTH_PRIVILEGED" >&2
  exit 1
fi

if grep -q 'android.permission.BLUETOOTH_STACK\|android.uid.system\|sharedUserId' "$ALLOWLIST"; then
  echo "::error::Unsafe privilege found in privapp allowlist" >&2
  exit 1
fi

PERM_COUNT=$(grep -c '<permission ' "$ALLOWLIST")
if [ "$PERM_COUNT" -ne 1 ]; then
  echo "::error::Privapp allowlist grants an unexpected number of permissions ($PERM_COUNT). Expected: 1" >&2
  exit 1
fi

TEXT_FILES="
$PROP_FILE
$MODULE_DIR/post-fs-data.sh
$MODULE_DIR/service.sh
$MODULE_DIR/customize.sh
$ALLOWLIST
$MODULE_DIR/META-INF/com/google/android/update-binary
$MODULE_DIR/META-INF/com/google/android/updater-script
"

for f in $TEXT_FILES; do
  if [ -f "$f" ] && grep -Eq 'android\.permission\.BLUETOOTH_STACK|android\.uid\.system|sharedUserId|(^|[[:space:]])setenforce([[:space:]]|$)|mount[[:space:]].*-o[[:space:]]*rw|pm[[:space:]]+clear[[:space:]]+com\.android\.bluetooth' "$f"; then
    echo "::error::Unsafe privileged-module directive found in $f" >&2
    exit 1
  fi
done

echo "BTAndroidTS Magisk module validation passed for package: $EXPECTED_PKG"
