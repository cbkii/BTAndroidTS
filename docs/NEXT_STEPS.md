# BTAndroidTS Next Steps

Audience: developers, reviewers, maintainers, and AI agents. This is an implementation planning
note; keep end-user instructions in `README.md`.

## Immediate Priorities

1. **Observed:** Keep CI unit-test execution aligned with the Android build workflow: install Android
   SDK 36/build-tools 36.0.0, write `local.properties`, and verify `aapt`/`apksigner` before Gradle
   test tasks.
2. **Observed:** Complete dedicated peripheral detail UI for saved/protected devices, HID state,
   retry/supervision controls, selected-device diagnostics, and confirmation-gated destructive
   actions.
3. **Observed:** Finish Android input verification UX and tests: `InputDevice` matching,
   no-match/ambiguous states, typing test field, keyboard-layout launcher/fallback copy, and
   persisted last-result evidence.
4. **Observed:** Harden HID Host capability reporting around profile proxy lifecycle, reflected API
   availability, typed failures, safe-mode blocking, and post-action state verification.
5. **Observed:** Extend OPP management without private contracts: bonded-device destination
   selection where public delegation permits, persisted request/history state, retry/cancel state for
   app-owned requests, and explicit “stock OPP owns progress” copy.
6. **Observed:** Finish read-only Topway tandem dashboard and conflict warnings without inferring
   connected phone/projection state from package presence alone.
7. **Observed:** Complete bounded diagnostics export and redaction tests, including app-external and
   `/storage/usbdiskN` targets.
8. **Observed:** Harden Magisk scripts and module validation for deterministic cleanup, release/debug
   APK package checks, allowlist shape, ZIP structure, and finite service behavior.
9. **Observed:** Add manifest/flavour verification coverage for privileged permission split, absence
   of `BLUETOOTH_STACK`, absence of shared UID, and safe exported flags.
10. **Requires device validation:** Run the TS18 real-device checklist before reporting runtime
    success for HID, OPP, Topway coexistence, boot/process recovery, ACC sleep/wake, or Magisk grant
    behavior.

## Safety Constraints Still Active

- **Unsupported:** replacing Bluetooth.apk, Bluetooth HAL/native libraries, WCN/controller firmware,
  Topway services, MCU/CAN/display/BOOT files, or vendor partitions.
- **Unsupported:** editing `/data/misc/bluedroid`, clearing all pairings, requesting
  `android.permission.BLUETOOTH_STACK`, using `sharedUserId="android.uid.system"`, broad SELinux
  rules, or root as the primary architecture.
- **Requires device validation:** TS18 runtime claims for SmartRemote, HOGP, phone-as-keyboard,
  input-node creation, keyboard layout, OPP send/receive, and Topway HFP/A2DP/PBAP/projection lanes.
