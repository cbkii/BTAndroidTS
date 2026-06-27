# raghavk92/Kontroller

- URL: https://github.com/raghavk92/Kontroller
- Clone: `https://github.com/raghavk92/Kontroller.git`
- Licence: Apache-2.0
- Priority: high
- Category: sender-side Android BluetoothHidDevice app

## Why this is included

Android phone as mouse/keyboard using the public Android 9+ Bluetooth HID Device profile. Useful for sender-app lifecycle, SDP registration and UX constraints.

## Use for

- phone-side setup checklist
- BluetoothHidDevice lifecycle examples
- keyboard/mouse report flow
- compatibility caveats

## Caution

Sender-side precedent only. Do not import UI or app assumptions into TS18 host workflow. Android phone must remain peripheral; TS18 remains host.

## Local snapshot

Run from the BTAndroidTS repo root:

```bash
bash docs/reference/fetch-reference-repos.sh
```

Expected source path after fetch:

```text
docs/reference/repos/kontroller/src
```
