# kshoji/BLE-HID-Peripheral-for-Android

- URL: https://github.com/kshoji/BLE-HID-Peripheral-for-Android
- Clone: `https://github.com/kshoji/BLE-HID-Peripheral-for-Android.git`
- Licence: Apache-2.0
- Priority: high
- Category: BLE/HOGP peripheral library

## Why this is included

Android BLE HID over GATT profile library; Android behaves as BLE mouse/keyboard/joystick. Useful for HOGP service/report evidence and host expectations.

## Use for

- BLE HID service UUID 0x1812
- GATT characteristic/report-map references
- HOGP compatibility guidance

## Caution

BTAndroidTS should not become a BLE HID peripheral; use this to recognise and classify phone apps that expose HOGP.

## Local snapshot

Run from the BTAndroidTS repo root:

```bash
bash docs/reference/fetch-reference-repos.sh
```

Expected source path after fetch:

```text
docs/reference/repos/ble-hid-peripheral-for-android/src
```
