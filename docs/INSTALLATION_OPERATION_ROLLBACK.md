# Installation, Operation, And Rollback

Audience: developers, reviewers, maintainers, and release builders. The public README gives the
end-user installation overview; this file records the controlled build, module, and rollback flow.

## Standard APK

Use the `standard` flavor for normal installation:

```bash
./gradlew assembleStandardRelease
adb install -r app/build/outputs/apk/standard/release/app-standard-release.apk
```

Runtime permissions required by normal operation:

- Bluetooth scan/connect/advertise where Android requires them.
- Fine location on Android 10/11 for Bluetooth scanning.

The standard APK must not request `BLUETOOTH_PRIVILEGED`, `BLUETOOTH_STACK`, UID 1000, platform
signing, shared UID, SELinux policy, or system partition writes.

## TS18 Privileged Magisk Variant

Build the privileged APK, place it in the module, then zip the module outside source control:

```bash
./gradlew assembleTs18PrivilegedRelease
sh scripts/package-magisk.sh
sh scripts/validate-magisk-package.sh
```

The packaging script copies
`app/build/outputs/apk/ts18Privileged/release/app-ts18Privileged-release.apk` to
`magisk/BTAndroidTS/system/priv-app/BTAndroidTS/BTAndroidTS.apk`, validates the package name and
allowlist, and creates `build/BTAndroidTS-magisk.zip`.

The module grants only `android.permission.BLUETOOTH_PRIVILEGED` through
`system/etc/permissions/privapp-permissions-com.cbkii.btandroidts.xml`. It does not replace
`Bluetooth.apk`, HAL libraries, firmware, Topway services, pairing databases, or platform-signed
packages.

Before first boot with the module enabled, preserve stock diagnostics, OTA/checksum evidence,
known-good boot images, exported settings, and recovery media.

## Operation

- Use Phone / Android Auto controls as vendor-owned Topway/ZLink status. Do not assign phone HFP,
  A2DP, AVRCP, PBAP, calls, contacts, or projection to BTAndroidTS by default.
- Use Keyboards / Peripherals for Android/Unisoc lane discovery and inventory.
- Use explicit actions: RFCOMM terminal, BLE GATT, HID Host, ACL, and OPP are separate connection
  types.
- Use File Sharing only through user-selected Android share intents and prefer the stock OPP path
  until TS18 runtime evidence proves it insufficient.
- Enable supervision only for explicitly saved, unprotected peripherals. Retry policies must be
  finite and visible.
- Run diagnostics only when user-started. Exports must be bounded, local-only, redacted, and safe
  for `/storage/usbdiskN` fallback when DocumentsUI is absent.

## Rollback

1. Disable the Magisk module or create the module `disable` marker.
2. Reboot.
3. Confirm `com.android.bluetooth`, `com.tw.bt`, ZLink, media buttons, calls, phone audio, and
   projection behave as stock.
4. Remove BTAndroidTS app data only after the module is disabled if a clean reset is needed.

STOP if recovery requires flashing, partition writes, Bluetooth stack replacement, Topway service
disablement, clearing all bonds, or vendor pairing database edits.
