# BTAndroidTS Release-Readiness Review

## Overall decision

BTAndroidTS is alpha-ready only after automated checks pass and the release notes clearly state that TS18 runtime behaviour still requires physical device validation.

<!-- pr12-roadmap-extension:start -->
## PR #12 comment-resolution and roadmap extension

Evidence label: Inferred.

This section extends the release-readiness review after resolving PR #11 and PR #12 review comments. It keeps the release scope honest: code paths may be implemented and testable in JVM/CI, but TS18 runtime behaviour remains `Requires device validation` until captured on the exact Topway/DoFun Android 10 head unit.

### Must finish before alpha release

- Keep PR #12 green for unit tests, lint, manifest permission checks, standard/privileged debug APK builds, Magisk packaging, ZIP validation, and shell syntax.
- Ensure PR #11 review comments are either superseded by PR #12, duplicate, stale/outdated, or intentionally left open with a clear reason.
- Keep PR #12 release notes explicit that HID Host, Android input-node creation, OPP stock delegation, Magisk privileged grant, USB export, ACC sleep/wake, and Topway/ZLink coexistence are not proven until TS18 physical validation is attached.
- Do not mark runtime rows `Observed` unless the repo contains exact-device evidence.

### Should finish before beta release

- Capture TS18 manual validation for classic keyboard pairing, BLE/HOGP peripheral pairing, SmartRemote behaviour, phone-as-keyboard attempts, HID input-node creation, keyboard layout configuration, OPP send/receive, diagnostics export, ACC sleep/wake, process death, adapter OFF/ON, Magisk rollback, Topway HFP/A2DP/PBAP, and wired/wireless ZLink/TLink coexistence.
- Add focused JVM tests for the review-comment fixes in this pass: failed keyboard verification retry, dynamic input-device refresh, non-keyboard input rejection, invalid peripheral-detail address state, and OPP status label mapping.
- Review release workflow artefact naming/signing once signing secrets are available.

### Can defer after alpha

- Dedicated phone-as-keyboard compatibility matrix.
- Extra adapter-boundary tests for OEM Bluetooth quirks.
- TS18 UI polish based on screenshots/photos from the head unit.
- Optional root-broker feature work; current safe default remains disabled/unsupported.

### Unsupported / not planned

- Replacing `Bluetooth.apk`, Bluetooth HAL/native libraries, WCN/controller firmware, Topway services, MCU/CAN/display/BOOT files, or partitions.
- Editing `/data/misc/bluedroid`, clearing all pairings, disabling vendor packages, requesting `BLUETOOTH_STACK`, using UID 1000/shared UID/platform signing, or adding broad SELinux policy.
<!-- pr12-roadmap-extension:end -->

