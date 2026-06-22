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
| 5 | Bonding/selective unpairing | Partial | Broadcast-waiting bond controller added; RFCOMM waits for bond. Selective unpair uses protected checks and hidden `removeBond`; exact privileged behavior requires TS18 validation. |
| 6 | Protect vendor/critical devices | Partial | Topway `CarKit_blink` address/name, likely vendor lane names and read-only Topway guardrails are protected. User protection now has persisted policy records; full UI controls remain incomplete. |
| 7 | Persistent supervision | Partial | Protobuf DataStore policy, saved peripherals, retry state, safe mode, boot/package/adapter receiver and foreground reconcile service are implemented. ACC sleep/wake and real reconnect behavior require TS18 validation. |
| 8 | Android 10 HID Host | Partial | Implemented code: isolated API-29 reflection adapter, privilege gates and narrow R8 rules. Automated tests: capability registry coverage exists; reflected HID failure-state tests are missing. TS18 runtime validation required: connected-device enumeration, connect/disconnect and input-node creation. Remaining development work: typed reflection failure coverage, proxy lifecycle reporting and post-action verification. |
| 9 | Android input creation | Partial | Input-device repository added. Typing test and keyboard-layout shortcut are pending. |
| 10 | Phone-as-keyboard/peripherals | Partial | Inventory/protection model supports Classic, BLE and peripheral classes. Compatibility remains unproven until TS18 tests. |
| 11 | OPP file sharing | Partial | Implemented code: SEND/SEND_MULTIPLE/text parsing, stock OPP delegation, local cancellation/retry and in-memory transfer history. Automated tests: transfer store coverage exists; parser/history edge cases are missing. TS18 runtime validation required: stock picker, destination selection and progress behavior. Remaining development work: persisted request/history store, history UI and clear stock-progress ownership copy. |
| 12 | Safe OPP fallbacks | Complete | No inbound OBEX server or private OPP bridge was added. Stock OPP remains the only outbound path until exact APK contracts prove insufficient. |
| 13 | Preserve Topway lane | Complete | Docs, protection policy and dashboard label phone/projection as vendor-owned. No Topway service writes added. |
| 14 | Topway conflict/tandem status | Partial | Vendor package inspector, Topway status model, lane guard and stock Topway Bluetooth launcher are implemented. Connected phone/projection state remains requires-validation. |
| 15 | TS18 dashboard | Partial | Dashboard buttons now route to stock phone UI, peripheral guidance, OPP guidance, manual supervisor reconcile, diagnostics export and Advanced tools. Dedicated detail screens remain incomplete. |
| 16 | Explicit connection types | Partial | Dashboard and docs separate RFCOMM terminal, BLE GATT, HID Host, ACL and OPP. Some inherited button labels still need a full UI copy pass. |
| 17 | Capability registry | Complete | Typed capability registry reports available/unavailable/privilege/root/failed/validation states. |
| 18 | Root broker | Partial | Fixed-operation root broker interface exists and is disabled by default. Enabled operations are pending. |
| 19 | Magisk module | Partial | Module skeleton, allowlist, bounded scripts, APK-copy/ZIP packaging script and validator are implemented. On-device privileged grant and rollback still require TS18 validation. |
| 20 | Avoid excessive authority | Complete | No `BLUETOOTH_STACK`, shared UID, UID 1000, platform signing or SELinux rules added. |
| 21 | Bounded diagnostics | Partial | Collector now includes app/build, policy, retry, HID/input, capability, root-broker disabled status, Topway package status and bounded local export with redaction. TS18 export behavior requires validation. |
| 22 | TS18 storage limits | Partial | App-external diagnostics export and `/storage/usbdiskN` target discovery are implemented. Direct USB write behavior requires TS18 validation. |
| 23 | Performance/lifecycle | Partial | Scanner singleton, receiver guards, foreground reconcile service, AlarmManager retry scheduling and safe-mode default avoid continuous scanning. OEM notification behavior requires TS18 validation. |
| 24 | Component security | Partial | Service is non-exported, reconcile receiver is non-exported, share intents are parsed/validated, and root broker remains disabled. URI-grant lifetime and destination UI still need deeper testing. |
| 25 | Automated testing | Partial | Implemented code: inventory/protection/capability, reconnect, Topway guard and OPP history tests. Automated tests present: JVM domain/store tests. Automated tests missing: Android service lifecycle, DataStore-on-device, HID reflection failure modes, OPP intent parsing, diagnostics export and manifest/flavour verification. TS18 runtime validation required: all hardware-facing behavior. Remaining development work: add the missing fake/unit tests and keep workflow setup aligned with Android build CI. |
| 26 | CI/release validation | Partial | Implemented code: Android build workflow covers unit tests, lint, debug/release variants, manifest checks, Magisk APK packaging and ZIP validation; unit-test workflow now mirrors SDK setup before Gradle. Automated tests present: workflow YAML/script syntax checks. TS18 runtime validation required: none for CI itself, but CI cannot prove hardware behavior. Remaining development work: add explicit manifest/flavour verification script and optional shellcheck/actionlint when available. |
| 27 | Install/operation/rollback docs | Complete | `docs/INSTALLATION_OPERATION_ROLLBACK.md` added. |
| 28 | Real-device validation matrix | Partial | Matrix is maintained with evidence rows; runtime HID/OPP/ACC/call/projection tests remain requiring TS18 validation. |
| 29 | Safety boundaries | Complete | Docs, policy and module avoid stack replacement, HAL/firmware changes, clear-all bonds and Topway disablement. |
| 30 | Completeness report | Complete | This file tracks current status; final response must include commits, tests and gaps. |
