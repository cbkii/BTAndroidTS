# BTTSplan analysis for BTAndroidTS reference work

## Observed from the attached plan

- The central feature is not normal phone discovery. The sender phone acts as a Bluetooth HID device/peripheral; the TS18 must act as HID Host.
- Android sender apps built on `BluetoothHidDevice` can disappear from the host workflow if they are not registered, foreground, advertising, or still connected to another host.
- BTAndroidTS should stop treating Classic and BLE as unrelated user flows for this use case. A dedicated Phone Keyboard Compatibility Mode should combine staged BLE scanning, Classic discovery where safe, candidate ranking, pairing, HID Host connection and input verification.
- `isConnectable == false` is not a safe visibility gate for this workflow. Such candidates should remain visible as “seen but not yet proven”.
- Pairing is not success. Success is HID connection plus Android input-device creation plus actual key events in the Keyboard Test field.

## Observed from current BTAndroidTS repository docs

- BTAndroidTS is already documented as a TS18 Android peripheral manager that leaves the factory Topway phone/projection lane alone.
- The repo already has a lane model: Android/Unisoc peripheral Bluetooth lane versus protected Topway automotive lane.
- The repo already requires exact TS18 validation before marking HID Host, phone-as-keyboard, input-node creation, OPP, ACC sleep/wake or coexistence as passed.
- The agent guide already warns not to claim unrun checks, not to request system UID/platform signing, and not to replace Bluetooth stack components.

## Inferred implementation consequence

The next major feature should be a dedicated `PhoneKeyboardCompatibilityMode`, not another generic scan tweak. It should collect evidence before action, preserve ambiguous candidates, use a typed failure taxonomy, and require input verification before a device is marked working.

## Reference research strategy

The selected references are split by job:

1. Android sender-side HID Device apps: inspect how phone keyboard apps register, stay foreground, create SDP/report descriptors and call `sendReport`.
2. BLE/HOGP peripheral projects: inspect service UUID 0x1812, Report Map and advertising patterns so BTAndroidTS can recognise candidates without over-filtering.
3. BLE scanning projects: inspect staged scan, extended advertising, PHY and callback handling without adopting continuous scans.
4. AOSP Android Bluetooth source/docs: inspect authority, hidden API and HID Host state boundaries.
5. BLE libraries: inspect state-machine and cancellation patterns, not as immediate dependencies.

## STOP conditions for agents

STOP before changing implementation if the proposed change:

- replaces `Bluetooth.apk`, Bluetooth HAL/native libraries, firmware, Topway services, MCU/CAN/display/BOOT/vendor partitions;
- clears all pairings or edits `/data/misc/bluedroid`;
- disables protected Topway/ZLink/SystemUI/MediaProvider/updater/Bluetooth packages;
- claims UID 1000, platform signing, `android.uid.system` or `BLUETOOTH_STACK` authority;
- marks TS18 runtime behaviour as passed without exact-device capture;
- converts phone-as-keyboard into a Topway phone/A2DP/PBAP/HFP workflow.
