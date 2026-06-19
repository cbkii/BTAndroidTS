# BTAndroidTS Implementation Status

Status terms:

- Complete: implemented in this branch and covered by code or docs.
- Partial: useful scaffolding exists, but feature behavior is not complete.
- Not started: no meaningful implementation yet.
- Blocked: local environment or missing device evidence prevents a pass claim.
- Requires TS18 validation: exact-device runtime capture is still required.

| # | Task | Status | Evidence / Remaining Work |
| --- | --- | --- | --- |
| 1 | Product baseline | Complete | Package/app identity and README/docs are `com.cbkii.btandroidts`; MIT license preserved. |
| 2 | Standard and privileged variants | Complete | `standard` and `ts18Privileged` flavors exist; privileged permission isolated to the flavor manifest. |
| 3 | Unified Bluetooth inventory | Partial | Domain inventory, merger, protection, lane ownership and app-scoped repository added. Profile state population still needs device adapters. |
| 4 | Bounded scan coordinator | Partial | App-scoped repository starts finite Classic/BLE sessions and singleton scanners avoid duplicate receiver registration. BLE/classic hardware behavior still needs TS18 validation. |
| 5 | Bonding/selective unpairing | Partial | Broadcast-waiting bond controller added; RFCOMM waits for bond. Selective unpair uses protected checks and hidden `removeBond`; exact privileged behavior requires TS18 validation. |
| 6 | Protect vendor/critical devices | Partial | Topway `CarKit_blink` address/name and likely vendor lane names are protected in policy. More user-protection persistence is pending. |
| 7 | Persistent supervision | Partial | Supervisor state/policy scaffolding added. Actual finite reconnect worker is pending and requires ACC validation. |
| 8 | Android 10 HID Host | Partial | API-29 reflection adapter is isolated and privilege-gated. Runtime HID connect/policy state requires TS18 validation. |
| 9 | Android input creation | Partial | Input-device repository added. Typing test and keyboard-layout shortcut are pending. |
| 10 | Phone-as-keyboard/peripherals | Partial | Inventory/protection model supports Classic, BLE and peripheral classes. Compatibility remains unproven until TS18 tests. |
| 11 | OPP file sharing | Partial | SEND/SEND_MULTIPLE entry activity and intent parser added. Destination UI, progress, cancellation, stock delegation and history are pending. |
| 12 | Safe OPP fallbacks | Not started | No custom OBEX client added. Stock OPP remains preferred until exact APK contracts are verified. |
| 13 | Preserve Topway lane | Complete | Docs, protection policy and dashboard label phone/projection as vendor-owned. No Topway service writes added. |
| 14 | Topway conflict/tandem status | Partial | Read-only lane ownership model exists. Launching stock phone UI and conflict warnings are pending. |
| 15 | TS18 dashboard | Partial | Home screen now has a TS18 dashboard band. Real actions for OPP, diagnostics, stock phone UI and supervision are pending. |
| 16 | Explicit connection types | Partial | Capability and docs separate RFCOMM, BLE GATT, HID, ACL and OPP. Existing older labels still need full UI pass. |
| 17 | Capability registry | Complete | Typed capability registry reports available/unavailable/privilege/root/failed/validation states. |
| 18 | Root broker | Partial | Fixed-operation root broker interface exists and is disabled by default. Enabled operations are pending. |
| 19 | Magisk module | Partial | Module skeleton, allowlist, bounded scripts and validator added. APK copy/zip build automation is pending. |
| 20 | Avoid excessive authority | Complete | No `BLUETOOTH_STACK`, shared UID, UID 1000, platform signing or SELinux rules added. |
| 21 | Bounded diagnostics | Partial | Redacted local collector scaffold exists. UI/export window and file writing are pending. |
| 22 | TS18 storage limits | Partial | Docs cover `/storage/usbdiskN`; implementation of export fallback is pending. |
| 23 | Performance/lifecycle | Partial | Scanner singleton and receiver guards reduce duplicates. More startup/service/notification hardening is pending. |
| 24 | Component security | Partial | Share activity is explicit and parses URI streams. URI persistence, root entry points and broadcast scoping need deeper audit. |
| 25 | Automated testing | Partial | Inventory/protection/capability tests added. Scanner, bonding, supervisor, HID, OPP, diagnostics and process recreation tests remain. |
| 26 | CI/release validation | Partial | CI already covers tests/lint/debug/release variants; manifest and Magisk validation scripts added. Local run is blocked by missing Android SDK. |
| 27 | Install/operation/rollback docs | Complete | `docs/INSTALLATION_OPERATION_ROLLBACK.md` added. |
| 28 | Real-device validation matrix | Partial | Matrix is maintained with evidence rows; runtime HID/OPP/ACC/call/projection tests remain requiring TS18 validation. |
| 29 | Safety boundaries | Complete | Docs, policy and module avoid stack replacement, HAL/firmware changes, clear-all bonds and Topway disablement. |
| 30 | Completeness report | Complete | This file tracks current status; final response must include commits, tests and gaps. |
