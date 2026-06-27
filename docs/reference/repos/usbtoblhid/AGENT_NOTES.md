# Agent notes for T-Dynamos/USBtoBLHid

Inspect this repository as precedent only. Do not change BTAndroidTS to depend on it unless a maintainer explicitly approves dependency adoption and TS18 validation.

## Suggested searches

```bash
grep -R "BluetoothHidDevice\|registerApp\|sendReport\|BluetoothProfile.HID_DEVICE" -n docs/reference/repos/usbtoblhid/src | head -100
grep -R "00001812\|HID_SERVICE\|Report Map\|ScanSettings\|setLegacy\|setPhy" -n docs/reference/repos/usbtoblhid/src | head -100
```

## Evidence handling

- Behaviour seen in this repo is precedent, not exact-device proof.
- If a pattern depends on privileged/root/platform behaviour, mark it Requires device validation or Unsupported for BTAndroidTS until proven safe.
- Preserve the Topway lane and BTAndroidTS safety boundaries.
