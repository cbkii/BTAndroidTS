# Agent reference navigation

## Before coding

1. Read `AGENTS.md`, `docs/BLUETOOTH_LANE_MODEL.md`, `docs/TS18_DEVICE_CONTEXT.md`, `docs/VALIDATION_MATRIX.md`, and this directory.
2. State whether each finding is Observed, Inferred, Hypothesis, Requires device validation, or Unsupported.
3. Keep sender-phone behaviour, TS18 HID Host behaviour, BLE scanning, Classic discovery, pairing, service discovery and input verification as separate layers.
4. Do not convert phone-keyboard work into normal phone/A2DP/HFP/PBAP/contacts/projection work.

## What to inspect by task

### Domain model / failure taxonomy

Use `PHONE_KEYBOARD_COMPATIBILITY_REFERENCES.md` and sender-side HID projects. Keep domain classes Android-light where possible.

### BLE scan hardening

Use official Android docs plus Nordic Scanner Compat and opendroneid scanner code. Implement staged scan passes with capability checks and bounded durations.

### HID Host connection

Use AOSP `HidHostService` and BTAndroidTS existing hidden/privileged adapter code. Do not claim hidden API calls work until TS18 validation proves them.

### UI guidance

Use sender-side HID apps to build a setup checklist, but keep BTAndroidTS wording host-side:

- open the phone keyboard/HID app;
- enable Bluetooth keyboard/server/HID mode;
- keep it foreground while pairing;
- ensure it is not already connected to another host;
- pair from BTAndroidTS Phone Keyboard mode;
- verify in Keyboard Test.

### Tests

Add fake scan/bond/HID/input states. Prioritise tests for candidate retention, non-connectable BLE visibility, HID evidence ranking, TTL expiry, Topway blocking, scan pass order, RSSI update throttling, and input verification requirement.

## Do not claim

- “validated on TS18” unless an exact-device capture proves it;
- “platform signed” or “UID 1000” from root/Magisk alone;
- “HID working” from bond creation alone;
- “phone found” from normal Classic phone discovery alone.

## Useful grep commands after fetching refs

```bash
grep -R "BluetoothHidDevice" -n docs/reference/repos/*/src | head -100
grep -R "registerApp" -n docs/reference/repos/*/src | head -100
grep -R "sendReport" -n docs/reference/repos/*/src | head -100
grep -R "00001812\|HID_SERVICE\|Report Map" -n docs/reference/repos/*/src | head -100
grep -R "setLegacy\|setPhy\|SCAN_MODE_LOW_LATENCY\|setReportDelay" -n docs/reference/repos/*/src | head -100
grep -R "isConnectable" -n docs/reference/repos/*/src | head -100
```
