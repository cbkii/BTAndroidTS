#!/usr/bin/env sh
# validate-magisk-package.sh [expected_package_name]
set -eu

EXPECTED_PKG="${1:-com.cbkii.btandroidts}"
MODULE_DIR="magisk/BTAndroidTS"
ALLOWLIST="$MODULE_DIR/system/etc/permissions/privapp-permissions-com.cbkii.btandroidts.xml"
APK="$MODULE_DIR/system/priv-app/BTAndroidTS/BTAndroidTS.apk"
PROP_FILE="$MODULE_DIR/module.prop"

echo "Validating Magisk module at $MODULE_DIR"

# 1. Essential files
for f in "$PROP_FILE" "$MODULE_DIR/post-fs-data.sh" "$MODULE_DIR/service.sh" "$MODULE_DIR/customize.sh" "$ALLOWLIST" "$APK"; do
  if [ ! -f "$f" ]; then
    echo "::error::Missing file: $f" >&2
    exit 1
  fi
done

# 2. Line endings (must be LF)
CR="$(printf '\r')"
if grep -Iq "$CR" "$PROP_FILE" "$MODULE_DIR/post-fs-data.sh" "$MODULE_DIR/service.sh" "$MODULE_DIR/customize.sh" "$ALLOWLIST"; then
  echo "::error::CRLF line endings detected in module files. Only LF is allowed." >&2
  exit 1
fi

# 3. module.prop validation
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

# 4. Privapp allowlist validation
# It must match the expected package (from aapt or default)
if ! grep -q "package=\"$EXPECTED_PKG\"" "$ALLOWLIST"; then
  echo "::error::Allowlist package mismatch. Expected: $EXPECTED_PKG" >&2
  grep "package=" "$ALLOWLIST" || true
  exit 1
fi

if ! grep -q 'android.permission.BLUETOOTH_PRIVILEGED' "$ALLOWLIST"; then
  echo "::error::Allowlist missing BLUETOOTH_PRIVILEGED" >&2
  exit 1
fi

# 5. Safety checks (block dangerous permissions/directives)
if grep -q 'android.permission.BLUETOOTH_STACK\|android.uid.system\|sharedUserId' "$ALLOWLIST"; then
  echo "::error::Unsafe privilege found in privapp allowlist" >&2
  exit 1
fi

# Limit to only intended permissions
PERM_COUNT=$(grep -c '<permission ' "$ALLOWLIST")
if [ "$PERM_COUNT" -ne 1 ]; then
  # For now, we only expect BLUETOOTH_PRIVILEGED
  echo "::error::Privapp allowlist grants an unexpected number of permissions ($PERM_COUNT). Expected: 1" >&2
  exit 1
fi

# Scan all scripts for dangerous commands
if grep -R -q 'BLUETOOTH_STACK\|android.uid.system\|sharedUserId\|setenforce\|mount -o rw\|pm clear com.android.bluetooth' "$MODULE_DIR"; then
  echo "::error::Unsafe privileged-module directive found in scripts" >&2
  exit 1
fi

echo "BTAndroidTS Magisk module validation passed for package: $EXPECTED_PKG"
