# opendroneid/receiver-android

- URL: https://github.com/opendroneid/receiver-android
- Clone: `https://github.com/opendroneid/receiver-android.git`
- Licence: Inspect upstream
- Priority: medium
- Category: Android BLE scanner app using extended advertising

## Why this is included

Real app precedent for extended BLE scanning, capability checks and low-latency scan settings. Useful for staged scan pass implementation.

## Use for

- setLegacy(false) guard patterns
- PHY capability checks
- foreground scan UX

## Caution

Different domain; only use scanner mechanics and validation mindset.

## Local snapshot

Run from the BTAndroidTS repo root:

```bash
bash docs/reference/fetch-reference-repos.sh
```

Expected source path after fetch:

```text
docs/reference/repos/opendroneid-receiver-android/src
```
