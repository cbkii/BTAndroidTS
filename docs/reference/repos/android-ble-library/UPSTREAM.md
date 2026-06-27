# NordicSemiconductor/Android-BLE-Library

- URL: https://github.com/NordicSemiconductor/Android-BLE-Library
- Clone: `https://github.com/NordicSemiconductor/Android-BLE-Library.git`
- Licence: BSD-3-Clause
- Priority: medium
- Category: BLE client/GATT architecture library

## Why this is included

Production-oriented Android BLE library for GATT connection management. It explicitly separates scanning from connection management and points to scanner compat for scanning.

## Use for

- GATT state machine structure
- reconnect/error taxonomy ideas
- bounded callback handling

## Caution

Do not use it as proof of TS18 runtime behaviour; dependency adoption requires validation and binary impact review.

## Local snapshot

Run from the BTAndroidTS repo root:

```bash
bash docs/reference/fetch-reference-repos.sh
```

Expected source path after fetch:

```text
docs/reference/repos/android-ble-library/src
```
