#!/usr/bin/env sh
set -eu

MODULE_DIR="magisk/BTAndroidTS"
ALLOWLIST="$MODULE_DIR/system/etc/permissions/privapp-permissions-com.cbkii.btandroidts.xml"

test -f "$MODULE_DIR/module.prop"
test -f "$MODULE_DIR/post-fs-data.sh"
test -f "$MODULE_DIR/service.sh"
test -f "$MODULE_DIR/customize.sh"
test -f "$ALLOWLIST"
test -d "$MODULE_DIR/system/priv-app/BTAndroidTS"

grep -q 'package="com.cbkii.btandroidts"' "$ALLOWLIST"
grep -q 'android.permission.BLUETOOTH_PRIVILEGED' "$ALLOWLIST"

if grep -R -q 'BLUETOOTH_STACK\|android.uid.system\|sharedUserId\|setenforce\|mount -o rw\|pm clear com.android.bluetooth' "$MODULE_DIR"; then
  echo "Unsafe privileged-module directive found" >&2
  exit 1
fi

echo "BTAndroidTS Magisk module structure is valid"
