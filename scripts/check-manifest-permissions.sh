#!/usr/bin/env sh
set -eu

MAIN_MANIFEST="app/src/main/AndroidManifest.xml"
PRIV_MANIFEST="app/src/ts18Privileged/AndroidManifest.xml"

if grep -q 'android.permission.BLUETOOTH_PRIVILEGED' "$MAIN_MANIFEST"; then
  echo "Main manifest must not request BLUETOOTH_PRIVILEGED" >&2
  return 1
fi

grep -q 'android.permission.BLUETOOTH_PRIVILEGED' "$PRIV_MANIFEST"

if command -v rg >/dev/null 2>&1; then
  if rg -q 'android\.permission\.BLUETOOTH_STACK|android\.uid\.system|sharedUserId' app/src/main app/src/ts18Privileged; then
    echo "Excessive Bluetooth/system authority requested" >&2
    return 1
  fi
else
  if grep -R -E -q 'android\.permission\.BLUETOOTH_STACK|android\.uid\.system|sharedUserId' app/src/main app/src/ts18Privileged; then
    echo "Excessive Bluetooth/system authority requested" >&2
    return 1
  fi
fi

if grep -q 'PeripheralSupervisorService' "$MAIN_MANIFEST"; then
  test -f app/src/main/java/com/cbkii/btandroidts/data/peripheral/PeripheralSupervisorService.kt
  grep -q 'android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE' "$MAIN_MANIFEST"
  grep -q 'android:foregroundServiceType="connectedDevice"' "$MAIN_MANIFEST"
fi

if grep -q 'BluetoothReconcileReceiver' "$MAIN_MANIFEST"; then
  test -f app/src/main/java/com/cbkii/btandroidts/data/peripheral/BluetoothReconcileReceiver.kt
  grep -q 'android.permission.RECEIVE_BOOT_COMPLETED' "$MAIN_MANIFEST"
fi

echo "BTAndroidTS manifest permission split is valid"
