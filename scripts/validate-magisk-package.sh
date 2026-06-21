#!/usr/bin/env sh
set -eu

MODULE_DIR="magisk/BTAndroidTS"
ALLOWLIST="$MODULE_DIR/system/etc/permissions/privapp-permissions-com.cbkii.btandroidts.xml"
APK="$MODULE_DIR/system/priv-app/BTAndroidTS/BTAndroidTS.apk"

test -f "$MODULE_DIR/module.prop"
test -f "$MODULE_DIR/post-fs-data.sh"
test -f "$MODULE_DIR/service.sh"
test -f "$MODULE_DIR/customize.sh"
test -f "$ALLOWLIST"
test -f "$APK"

grep -q 'package="com.cbkii.btandroidts"' "$ALLOWLIST"
grep -q 'android.permission.BLUETOOTH_PRIVILEGED' "$ALLOWLIST"

if grep -q 'android.permission.BLUETOOTH_STACK\|android.uid.system\|sharedUserId' "$ALLOWLIST"; then
  echo "Unsafe privilege in privapp allowlist" >&2
  exit 1
fi

if [ "$(grep -c '<permission ' "$ALLOWLIST")" -ne 1 ]; then
  echo "Privapp allowlist must grant exactly one permission" >&2
  exit 1
fi

if grep -R -q 'BLUETOOTH_STACK\|android.uid.system\|sharedUserId\|setenforce\|mount -o rw\|pm clear com.android.bluetooth' "$MODULE_DIR"; then
  echo "Unsafe privileged-module directive found" >&2
  exit 1
fi

echo "BTAndroidTS Magisk module structure is valid"
