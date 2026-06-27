# Alkaid-Benetnash/EmuBTHID

- URL: https://github.com/Alkaid-Benetnash/EmuBTHID
- Clone: `https://github.com/Alkaid-Benetnash/EmuBTHID.git`
- Licence: Inspect upstream
- Priority: low-medium
- Category: BlueZ Classic Bluetooth HID Device emulator

## Why this is included

Classic Bluetooth HID Device precedent on Linux/BlueZ; registers HID service UUID 00001124 and exposes keyboard/mouse. Useful to distinguish Classic HID from BLE/HOGP.

## Use for

- Classic HID terminology
- HID SDP/service UUID
- host expectations

## Caution

Linux/BlueZ/root precedent only. Do not apply BlueZ commands or privileged port assumptions to TS18 Android.

## Local snapshot

Run from the BTAndroidTS repo root:

```bash
bash docs/reference/fetch-reference-repos.sh
```

Expected source path after fetch:

```text
docs/reference/repos/emubthid/src
```
