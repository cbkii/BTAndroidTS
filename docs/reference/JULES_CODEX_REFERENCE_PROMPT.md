# Jules/Codex prompt addendum: phone-keyboard reference use

Use this as an addendum to the main implementation prompt.

```text
You are working in cbkii/BTAndroidTS.

Before coding, inspect:
- AGENTS.md
- docs/BLUETOOTH_LANE_MODEL.md
- docs/TS18_DEVICE_CONTEXT.md
- docs/VALIDATION_MATRIX.md
- docs/reference/README.md
- docs/reference/BTTSPLAN_ANALYSIS.md
- docs/reference/PHONE_KEYBOARD_COMPATIBILITY_REFERENCES.md
- docs/reference/REFERENCE_MANIFEST.md
- docs/reference/AGENT_REFERENCE_NAVIGATION.md

If docs/reference/repos/*/src is missing, continue from the manifest and official URLs; do not stop to ask questions. Treat unfetched upstream repos as references to be inspected when available, not as blockers.

Goal:
Implement a dedicated Phone Keyboard Compatibility Mode for Android phones running Bluetooth HID keyboard apps. The phone is the HID Device/peripheral. BTAndroidTS/TS18 is the HID Host.

Hard constraints:
- Keep Topway phone/projection/audio/contacts lane protected.
- Do not replace Bluetooth.apk, HAL, native libraries, firmware, Topway services or pairing DBs.
- Do not clear all pairings.
- Do not claim platform signing, UID 1000, BLUETOOTH_STACK or vendor identity.
- Do not mark TS18 runtime behaviour passed without exact-device capture.
- Pairing alone is not success; input events in Keyboard Test are success.

Reference-use rules:
- Use sender-side HID Device repos to understand app lifecycle and phone setup checklist.
- Use BLE/HOGP repos to understand candidate evidence and service/report-map patterns.
- Use scanner repos to design staged BLE scans with bounded duration and fallback.
- Use AOSP/official docs to confirm authority, API and hidden/privileged boundaries.
- Do not copy code blindly; preserve licences if adapting any non-trivial code.

Implement minimal, testable commits:
1. PhoneKeyboardCandidate/evidence/failure domain model.
2. BLE-first staged scan controller with no connectable-only filtering.
3. Candidate ranking and UI list with evidence and guidance.
4. Pair/bond/connect/verify flow through existing isolated HID Host controller.
5. Failure taxonomy mapped to user guidance.
6. Sender-side setup checklist/help panel.
7. Unit tests/fakes for scan/candidate/ranking/failure/input-verification cases.
8. Docs/status/validation matrix updates.

Run or record exact failures for:
- ./gradlew testStandardDebugUnitTest
- ./gradlew lintStandardDebug
- ./gradlew assembleStandardDebug
- ./gradlew assembleStandardRelease
- ./gradlew assembleTs18PrivilegedRelease
- sh scripts/check-manifest-permissions.sh
- sh scripts/package-magisk.sh
- sh scripts/validate-magisk-package.sh

Final report must include files changed, tests run, exact failures, unrun checks, and TS18 validation still required.
```
