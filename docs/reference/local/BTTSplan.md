**Observed:** This is not a “normal Android phone discovery” problem. For the target use case, the phone is acting as a **Bluetooth HID device/peripheral**, while the TS18 must act as the **HID host**. The TS18 board documentation supports the hardware precedent: built-in Bluetooth 5.0 lists external Bluetooth OBD, mice, game controllers, and keyboards as supported device types. 

**Observed in current BTAndroidTS:** the previous quick patch addressed two obvious blockers: the dashboard consuming vertical space, and the BLE scanner dropping `isConnectable == false` advertisements. But to make phone-as-keyboard reliable, BTAndroidTS needs a more deliberate compatibility layer, not just “scan BLE harder”.

## Research findings that matter

**1. Android phone keyboard apps usually expose HID Device, not a normal phone profile.**
Android’s public `BluetoothHidDevice` API is for the sending phone/app side. The sender app registers an HID Device application with an SDP record; Android explicitly says HID Device connections are only possible while the app is registered, only one app can be registered at a time, registration disables the sending phone’s HID Host service until unregistered, and the app may be automatically unregistered if it is not foreground. That means BTAndroidTS should guide the user to keep the phone keyboard app open/foreground/advertising during scan and pairing. ([Android Developers][1])

**2. The TS18 must detect both Bluetooth Classic HID and BLE/HOGP candidates.**
Bluetooth HID exists as Classic HID, and HOGP is HID over GATT for Bluetooth Low Energy. Android keyboard apps may use either path depending on Android version, vendor stack, app implementation, and permissions. Treating “Android phone” as one scan target is wrong; BTAndroidTS should classify candidates as Classic HID, BLE/HOGP, or unknown HID-capable. ([Wikipedia][2])

**3. The HID report path matters after pairing.**
On the sending phone side, Android’s `BluetoothHidDevice.sendReport()` sends HID input reports over the interrupt channel, and report IDs must match the descriptor. On the TS18 side, BTAndroidTS cannot prove success merely because a phone paired; it must verify `InputDevice` creation and actual key events in the keyboard-test field. ([Android Developers][1])

**4. BLE scan defaults can miss or defer useful results.**
Android’s BLE scanner can use default scans, explicit filters/settings, callback scans, or PendingIntent scans. Unfiltered scans can stop on screen-off; Android recommends using filtered scans with desired settings to avoid that behaviour. Scan settings also expose low-latency mode, legacy-vs-extended advertisement handling, PHY selection, and report delay; report delay `0` means immediate delivery. ([Android Developers][3]) ([Android Developers][4]) ([Android Developers][4])

**5. Do not rely on `isConnectable` as a visibility gate.**
For this use case, `isConnectable == false` is not a safe reason to hide a phone. Some advertisements may be incomplete, non-connectable, privacy-addressed, extended, or not advertising all GATT services until pairing/connection. BTAndroidTS should display “seen but not yet connectable/proven” rather than filtering the device out.

**6. Debug/logging must stay bounded.**
The TS18/8581 debug docs warn that Ylog/debug settings can affect system speed and create junk/log files during sleep; this reinforces the earlier requirement that BTAndroidTS normal runtime logging should be silent unless debug mode or user-started diagnostics is active. 

## What similar projects/apps imply

**Observed / public Android API pattern:** phone keyboard apps built on `BluetoothHidDevice` generally need a foreground UI/service, a registered HID device SDP record, a keyboard report descriptor, and explicit report sending. BTAndroidTS should therefore add a **sender-app compatibility checklist**: app foreground, advertising on, Bluetooth on, not already connected elsewhere, “pair from receiver/host”, and retry after toggling advertising only, not the TS18 Bluetooth stack. ([Android Developers][1])

**Inferred from HID/HOGP design:** compatibility depends more on service/report/profile state than on friendly device names. A phone may not appear as “Pixel” or “Samsung”; it may appear as the app’s HID name, a random/private BLE address, an unnamed BLE device, or only after service discovery. BTAndroidTS should stop expecting Android phones to show as normal phone-class Classic devices.

**Observed from current codebase risk:** the current scanner model still treats Classic and BLE tabs as separate user flows. That is not good enough for the primary phone-keyboard flow. A “Phone keyboard scan” should run a staged scan pipeline and show candidates from both transports in one compatibility list.

## Recommended next implementation scope

### Priority 1 — Add a proper Phone Keyboard Compatibility Mode

**Action:** create a dedicated workflow, separate from generic Classic/BLE tabs:

```text
PhoneKeyboardCompatibilityMode
PhoneKeyboardScanController
PhoneKeyboardCandidate
PhoneKeyboardPairingGuide
PhoneKeyboardValidationState
```

It should run this sequence:

```text
1. Check TS18 Bluetooth ON and permissions.
2. Refresh bonded devices.
3. Start BLE low-latency scan, no service filter, no connectable filter.
4. Start Classic discovery only if safe and not already scanning BLE.
5. Record all seen candidates, including unnamed/private-address BLE devices.
6. Classify candidates:
   - already bonded;
   - advertises HID service 0x1812;
   - has HID-ish name keywords;
   - BLE only, service unknown;
   - Classic keyboard/device class;
   - Topway/vendor protected;
   - normal phone profile risk.
7. Let user select a candidate.
8. Pair/bond.
9. Refresh UUIDs/services.
10. Attempt HID Host connection only through the isolated TS18 HID Host controller.
11. Verify Android input device.
12. Open Keyboard Test and require actual typed input before marking success.
```

**Why:** phone keyboard apps may advertise as BLE HID peripherals rather than normal discoverable phones, and Android’s sender-side HID app must be foreground/registered. ([Android Developers][1])

### Priority 2 — Replace “device not found” with staged evidence

Current UX likely makes users think the app failed when the phone is simply not advertising the right profile yet. The UI should show:

```text
No phone-keyboard candidates yet
Checklist:
- Open the Bluetooth Keyboard app on the phone
- Enable its Bluetooth/HID/advertising mode
- Keep the app foreground
- Remove stale pairing on both sides if previously failed
- Tap Rescan
```

For each candidate, show evidence:

```text
Name: Pixel Keyboard / Unknown BLE / Bluetooth Keyboard
Address: redacted by default
Transport: BLE / Classic / Both
RSSI: sampled, not high-frequency
Bond: Not bonded / Bonded
HID service: Advertised / Unknown / Not seen
Input node: Not tested / Active / Failed
Topway risk: Protected / Low / Unknown
Recommended action: Pair / Connect HID / Test input / Ignore
```

### Priority 3 — Implement multi-pass BLE scanning

Use at least two bounded BLE passes:

```text
Pass A: low-latency, no filter, immediate results, legacy=true
Pass B: low-latency, no filter, immediate results, legacy=false + all supported PHY where supported
```

On Android 10/API 29, `setLegacy(false)` and PHY selection are API 26+ APIs, but selecting unsupported PHY can fail, so capability-check and fall back to legacy scanning. ([Android Developers][4])

Do not set a HID-service-only filter as the only scan path. Use it only as an optional second-stage verification because some phones/apps may not advertise 0x1812 until connected or bonded.

### Priority 4 — Improve candidate preservation

BTAndroidTS should persist short-lived “seen” candidates for a bounded window, even if later scan callbacks do not repeat them.

Add:

```text
firstSeenAt
lastSeenAt
seenCount
lastRssi
advertisedName
scanRecordName
serviceUuids
manufacturerDataPresent
serviceDataPresent
isConnectable = true / false / unknown
addressType = public / random / unknown
```

Do not update Compose state on every RSSI change; sample or only update when meaningful to avoid recomposition churn, which is already called out in the original TS18 requirements. 

### Priority 5 — Add pairing failure taxonomy

For phone keyboard apps, “pair failed” is not enough. Add typed outcomes:

```text
PHONE_APP_NOT_ADVERTISING
BLE_SEEN_NOT_CONNECTABLE
PAIRING_TIMEOUT
PAIRING_REJECTED_BY_PHONE
BOND_CREATED_NO_HID_SERVICE
HID_SERVICE_SEEN_CONNECT_FAILED
HID_CONNECTED_NO_INPUT_DEVICE
INPUT_DEVICE_ACTIVE_NO_KEY_EVENTS
TOPWAY_CONFLICT_RISK
PRIVILEGED_HID_HOST_REQUIRED
UNSUPPORTED_TS18_STACK
```

This lets the UI give useful recovery steps instead of blind rescans.

### Priority 6 — Add exact compatibility guidance for common sender apps

Create a data-only compatibility table, not hard-coded behaviour:

```kotlin
data class PhoneKeyboardAppGuide(
    val displayName: String,
    val expectedTransport: ExpectedTransport,
    val expectedDeviceNameHints: List<String>,
    val setupSteps: List<String>,
    val knownFailureModes: List<String>,
)
```

Initial entries should be generic:

```text
Generic Android Bluetooth Keyboard app
Expected: BLE/HOGP or Classic HID
Steps: open app → enable Bluetooth keyboard/server/HID mode → keep foreground → pair from TS18

Android app using BluetoothHidDevice API
Expected: sender app must remain registered/foreground
Failure: sender auto-unregisters when backgrounded
```

Only add named apps after inspecting them or after user/device validation.

### Priority 7 — Add a sender-side diagnostic recipe

Since the phone app is half of the system, BTAndroidTS should include a “phone-side evidence” checklist:

```text
Phone model / Android version
Keyboard app name/version
Whether app says HID registered / advertising / connected
Whether phone asks for pairing confirmation
Whether another host is already connected
Whether phone Bluetooth pairing list contains TS18
Whether app must stay foreground
```

This is not generic phone advice; it follows directly from Android’s HID Device API lifecycle. ([Android Developers][1])

### Priority 8 — Strengthen tests with fake BLE/HID candidates

Add unit tests for:

```text
unnamed BLE device is retained;
isConnectable=false is still retained;
HID UUID 0x1812 candidate ranks higher;
private-address candidate remains visible;
Classic + BLE same MAC merge;
Classic + BLE different random address do not merge incorrectly;
stale candidates expire after bounded TTL;
Topway protected devices cannot be selected for phone-keyboard mode;
phone-keyboard scan starts BLE-first;
tab switching does not cancel scan;
RSSI-only updates are rate-limited;
pairing failure taxonomy maps to user guidance;
input verification is required before success.
```

## Jules/Codex follow-up prompt

```text
Jules, continue on cbkii/BTAndroidTS from latest main or the current phone-keyboard branch.

Goal:
Harden the TS18 “Android phone as Bluetooth keyboard” workflow so Android phones running Bluetooth Keyboard/HID apps are reliably discovered, paired, connected as HID where supported, and verified through Android input events.

Do not treat the phone as a normal Topway phone/A2DP/HFP/PBAP device. The phone-keyboard use case is a peripheral/HID use case. Preserve the Topway automotive Bluetooth lane.

Current user-observed problems:
- the dashboard button rail/layout was obstructing the device view;
- partially implemented buttons are still confusing;
- generic scanning finds small BT devices but misses Android phones running keyboard apps;
- phone-as-keyboard support is the top priority.

Research findings to apply:
- Android keyboard apps commonly use Android’s BluetoothHidDevice role on the sending phone.
- The sending app must register HID Device state; HID Device connections are possible only while registered; only one app can register at a time; registration disables the sender phone’s HID Host until unregistered; and the app can be unregistered if not foreground.
- BTAndroidTS/TS18 must act as HID Host and verify Android input-node creation, not just pairing.
- Phone keyboard apps may advertise as BLE/HOGP, Classic HID, unnamed BLE, private-address BLE, or app-named devices.
- Do not hide BLE results just because `isConnectable == false`.
- Do not rely only on device names or Android phone device class.
- Do not run noisy runtime logging unless explicit debug mode or user-started diagnostics is active.

Implement the following focused scope.

Commit 1 — Add phone keyboard compatibility domain model
Create:
- PhoneKeyboardCandidate
- PhoneKeyboardScanEvidence
- PhoneKeyboardCompatibilityMode
- PhoneKeyboardFailureReason
- PhoneKeyboardUserGuidance

Track:
- firstSeenAt;
- lastSeenAt;
- seenCount;
- transport Classic/BLE/Both;
- display name;
- raw advertised name;
- service UUIDs;
- HID service 0x1812 evidence;
- RSSI sampled;
- connectable true/false/unknown;
- bonded state;
- protected/Topway risk;
- HID profile state;
- Android input verification state.

Commit 2 — Replace single BLE scan path with staged phone-keyboard scan
Add a dedicated Phone Keyboard Scan action that:
- starts BLE-first;
- uses low-latency scan;
- uses immediate report delivery;
- does not filter out non-connectable results;
- performs legacy and extended advertisement passes where supported;
- falls back safely if setLegacy(false) or PHY selection fails;
- optionally runs Classic discovery as a second stage;
- never performs continuous discovery;
- has bounded duration;
- preserves seen candidates for a bounded TTL.

Do not use HID-service-only filtering as the only scan path. Use service UUID 0x1812 only as ranking/evidence.

Commit 3 — Add candidate ranking and UI
Show a unified “Phone keyboard candidates” list, separate from generic Classic/BLE tabs.

Rank:
1. bonded + HID service/input evidence;
2. HID service 0x1812 advertised;
3. keyboard/HID name hints;
4. BLE candidates seen during phone-keyboard mode;
5. Classic keyboard/device-class candidates;
6. unknown candidates.

Each row must show:
- name or Unknown BLE device;
- transport;
- evidence;
- bond state;
- recommended action;
- failure/recovery hint.

Commit 4 — Add pairing/connect/verify flow
For a selected candidate:
- pair/bond with awaited state;
- refresh UUID/service evidence;
- connect HID Host through the existing isolated HID Host controller where available;
- verify Android InputDevice creation;
- require keyboard-test input before marking success;
- persist last successful verification.

Do not mark a phone as “working” from pairing alone.

Commit 5 — Add failure taxonomy and guidance
Add typed results:
- PHONE_APP_NOT_ADVERTISING
- BLE_SEEN_NOT_CONNECTABLE
- PAIRING_TIMEOUT
- PAIRING_REJECTED_BY_PHONE
- BOND_CREATED_NO_HID_SERVICE
- HID_SERVICE_SEEN_CONNECT_FAILED
- HID_CONNECTED_NO_INPUT_DEVICE
- INPUT_DEVICE_ACTIVE_NO_KEY_EVENTS
- TOPWAY_CONFLICT_RISK
- PRIVILEGED_HID_HOST_REQUIRED
- UNSUPPORTED_TS18_STACK

Map each to clear UI guidance.

Commit 6 — Add phone-side setup checklist
Add a small help panel:
- open the Android Bluetooth Keyboard/HID app;
- enable its Bluetooth keyboard/server/HID mode;
- keep the app foreground while pairing;
- make sure it is not already connected to another host;
- remove stale pairing on phone and TS18 only for that device if needed;
- pair from BTAndroidTS phone-keyboard mode;
- test input in BTAndroidTS Keyboard Test.

Do not provide generic phone Bluetooth advice as the primary solution.

Commit 7 — Add tests
Add unit tests/fakes for:
- unnamed BLE candidate retained;
- non-connectable BLE candidate retained;
- HID UUID ranks higher;
- private/random address candidate visible;
- candidate TTL expiry;
- Classic/BLE merge rules;
- Topway protected candidate blocked;
- phone-keyboard scan is BLE-first;
- tab switching does not cancel scan;
- RSSI-only updates are rate-limited;
- pairing failure taxonomy;
- input verification required before success.

Commit 8 — Documentation and validation
Update:
- README.md
- docs/IMPLEMENTATION_STATUS.md
- docs/VALIDATION_MATRIX.md
- docs/NEXT_STEPS.md

Document:
- phone-as-keyboard is the primary target;
- generic Android phones may not appear as normal Classic phone devices;
- sender app must remain foreground/registered;
- HID success requires Android input verification;
- TS18 real-device validation remains required until tested.

Validation commands:
- ./gradlew --no-daemon --stacktrace --warning-mode all :app:testStandardDebugUnitTest
- ./gradlew --no-daemon --stacktrace --warning-mode all :app:lintStandardDebug
- sh scripts/check-manifest-permissions.sh
- ./gradlew --no-daemon --stacktrace --warning-mode all :app:assembleStandardDebug :app:assembleTs18PrivilegedDebug

Final report:
- commits made;
- files changed;
- tests run;
- failures fixed;
- TS18 validation still required;
- exact remaining gaps.
```

**Bottom line:** the earlier patch was a useful first correction, but the stronger fix is a dedicated “phone keyboard compatibility mode” with BLE-first discovery, no premature filtering, candidate evidence/ranking, a guided sender-app checklist, HID Host connection, and input verification. Pairing alone is not success; typed input on the TS18 is success.

[1]: https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice "BluetoothHidDevice  |  API reference  |  Android Developers"
[2]: https://en.wikipedia.org/wiki/List_of_Bluetooth_profiles?utm_source=chatgpt.com "List of Bluetooth profiles"
[3]: https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner "BluetoothLeScanner  |  API reference  |  Android Developers"
[4]: https://developer.android.com/reference/android/bluetooth/le/ScanSettings.Builder "ScanSettings.Builder  |  API reference  |  Android Developers"
