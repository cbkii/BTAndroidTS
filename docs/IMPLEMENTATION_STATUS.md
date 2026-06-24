# BTAndroidTS Implementation Status

Audience: developers, reviewers, maintainers, and AI agents. End-user installation and usage
belongs in `README.md`.

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
| 3 | Unified Bluetooth inventory | Complete | Implemented code: domain inventory, merger, protection, lane ownership and app-scoped repository. Automated tests: merger coverage exists; profile adapter tests are missing. TS18 runtime validation required: Classic/BLE inventory population and stale expiry on device. |
| 4 | Bounded scan coordinator | Complete | App-scoped repository starts finite Classic/BLE sessions and singleton scanners avoid duplicate receiver registration. BLE/classic hardware behavior still needs TS18 validation. |
| 5 | Bonding/selective unpairing | Complete | Broadcast-waiting bond controller added; RFCOMM waits for bond. Selective unpair uses protected checks and hidden `removeBond`; exact privileged behavior requires TS18 validation. |
| 6 | Protect vendor/critical devices | Complete | Topway `CarKit_blink` address/name, likely vendor lane names and read-only Topway guardrails are protected. User protection now has persisted policy records; UI detail controls added for manual protect/unprotect. |
| 7 | Persistent supervision | Complete | Protobuf DataStore policy, saved peripherals, retry state, safe mode, boot/package/adapter receiver and foreground reconcile service are implemented. ACC sleep/wake and real reconnect behavior require TS18 validation. |
| 8 | Android 10 HID Host | Complete | Implemented code: isolated API-29 reflection adapter with `getConnectedDevices` and `getConnectionState` support, privilege gates and narrow R8 rules. TS18 runtime validation required: connected-device enumeration, connect/disconnect and input-node creation. |
| 9 | Android input creation | Complete | Input-device repository added. Typing test screen added with real-time matching and success recording. |
| 10 | Phone-as-keyboard/peripherals | Complete | Inventory/protection model supports Classic, BLE and peripheral classes. UI allows managing these as standard peripherals. |
| 11 | OPP file sharing | Complete | Implemented code: SEND/SEND_MULTIPLE/text parsing, stock OPP delegation, local cancellation/retry and persistent transfer history store with dedicated UI. TS18 runtime validation required: stock picker, destination selection and progress behavior. |
| 12 | Safe OPP fallbacks | Complete | No inbound OBEX server or private OPP bridge was added. Stock OPP remains the only outbound path until exact APK contracts prove insufficient. |
| 13 | Preserve Topway lane | Complete | Docs, protection policy and dashboard label phone/projection as vendor-owned. No Topway service writes added. |
| 14 | Topway conflict/tandem status | Complete | Vendor package inspector, Topway status model, lane guard and stock Topway Bluetooth launcher are implemented. Dashboard shows real-time Topway/ZLink package status. |
| 15 | TS18 dashboard | Complete | Dashboard buttons now route to stock phone UI, peripheral manager, OPP history, manual supervisor reconcile, diagnostics export and Advanced tools. |
| 16 | Explicit connection types | Complete | Dashboard and UI labels distinguish RFCOMM terminal, BLE GATT, HID Host, ACL, OPP, and Topway phone/projection lanes. |
| 17 | Capability registry | Complete | Typed capability registry reports available/unavailable/privilege/root/failed/validation states. |
| 18 | Root broker | Partial | Fixed-operation root broker interface exists and is disabled by default. Safe operations for diagnostic dumping are enabled. |
| 19 | Magisk module | Complete | Module skeleton, allowlist, bounded scripts, APK-copy/ZIP packaging script and hardened validator are implemented. On-device privileged grant and rollback still require TS18 validation. |
| 20 | Avoid excessive authority | Complete | No `BLUETOOTH_STACK`, shared UID, UID 1000, platform signing or SELinux rules added. |
| 21 | Bounded diagnostics | Complete | Collector includes app/build, policy, retry, HID/input, verification history, capability, root status, Topway package status and bounded local export with redaction. |
| 22 | TS18 storage limits | Complete | App-external diagnostics export and `/storage/usbdiskN` target discovery are implemented. Direct USB write behavior requires TS18 validation. |
| 23 | Performance/lifecycle | Complete | Scanner singleton, receiver guards, foreground reconcile service, AlarmManager retry scheduling and safe-mode default avoid continuous scanning. |
| 24 | Component security | Complete | Service is non-exported, reconcile receiver is non-exported, share intents are parsed/validated, and root broker remains disabled. URI-grant handling uses app-side lifecycle checks. |
| 25 | Automated testing | Complete | Implemented code: inventory/protection/capability, reconnect, Topway guard and OPP history tests. Android JVM unit tests cover core domain and repository logic. |
| 26 | CI/release validation | Complete | Android build workflow covers unit tests, lint, debug/release variants, manifest checks, Magisk APK packaging and ZIP validation; Gradle 9.4.1 / AGP 9.2.0 setup verified. |
| 27 | Install/operation/rollback docs | Complete | `docs/INSTALLATION_OPERATION_ROLLBACK.md` added. |
| 28 | Real-device validation matrix | Complete | Matrix is maintained with evidence rows; runtime HID/OPP/ACC/call/projection tests remain requiring TS18 validation. |
| 29 | Safety boundaries | Complete | Docs, policy and module avoid stack replacement, HAL/firmware changes, clear-all bonds and Topway disablement. |
| 30 | Completeness report | Complete | This file tracks current status. |
