# T-Dynamos/USBtoBLHid

- URL: https://github.com/T-Dynamos/USBtoBLHid
- Clone: `https://github.com/T-Dynamos/USBtoBLHid.git`
- Licence: MIT
- Priority: medium-high
- Category: Android USB HID to Bluetooth HID Device bridge

## Why this is included

Android registers as Bluetooth HID Device, listens to USB keyboard/mouse input and sends HID reports over Bluetooth. Useful for report conversion and connected-host state handling.

## Use for

- sendReport/report-map patterns
- input-to-HID conversion
- foreground service behaviour

## Caution

This is a sender/bridge app, not a TS18 HID Host implementation. Do not adapt USB-host assumptions to BTAndroidTS host pairing flow.

## Local snapshot

Run from the BTAndroidTS repo root:

```bash
bash docs/reference/fetch-reference-repos.sh
```

Expected source path after fetch:

```text
docs/reference/repos/usbtoblhid/src
```
