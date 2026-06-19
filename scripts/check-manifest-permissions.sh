#!/usr/bin/env sh
set -eu

MAIN_MANIFEST="app/src/main/AndroidManifest.xml"
PRIV_MANIFEST="app/src/ts18Privileged/AndroidManifest.xml"

if grep -q 'android.permission.BLUETOOTH_PRIVILEGED' "$MAIN_MANIFEST"; then
  echo "Main manifest must not request BLUETOOTH_PRIVILEGED" >&2
  exit 1
fi

grep -q 'android.permission.BLUETOOTH_PRIVILEGED' "$PRIV_MANIFEST"

if grep -R -q 'android.permission.BLUETOOTH_STACK\|android.uid.system\|sharedUserId' app/src/main app/src/ts18Privileged; then
  echo "Excessive Bluetooth/system authority requested" >&2
  exit 1
fi

echo "BTAndroidTS manifest permission split is valid"
