# NordicSemiconductor/Android-Scanner-Compat-Library

- URL: https://github.com/NordicSemiconductor/Android-Scanner-Compat-Library
- Clone: `https://github.com/NordicSemiconductor/Android-Scanner-Compat-Library.git`
- Licence: BSD-3-Clause
- Priority: high
- Category: BLE scanning compatibility library

## Why this is included

A long-lived BLE scanner compatibility library handling Android API changes, hardware filtering/batching and scan settings. Useful for staged scan design and fallback thinking.

## Use for

- scan settings compatibility
- bounded scan callbacks
- match-lost/device TTL ideas
- legacy vs extended advertising handling

## Caution

Do not add a dependency without size/API29/TS18 validation. Inspect as design reference first.

## Local snapshot

Run from the BTAndroidTS repo root:

```bash
bash docs/reference/fetch-reference-repos.sh
```

Expected source path after fetch:

```text
docs/reference/repos/android-scanner-compat-library/src
```
