# BTAndroidTS TS18 Consolidated Development Guide

Audience: developers, reviewers, maintainers, and AI agents working on `cbkii/BTAndroidTS`.

This file consolidates the legacy TS18 docs into one current development reference. It replaces the separated legacy files for device context, Bluetooth lane model, implementation status, validation matrix, and installation/rollback notes. Keep `README.md` short and user-facing. Keep hard repository rules in `AGENTS.md`. Keep this file focused on current engineering state, remaining plans, validation, release readiness, and exact-device boundaries.

## 1. Project Purpose

BTAndroidTS is a Bluetooth peripheral manager for Topway TS18 Android head units. It exists to manage Android Bluetooth peripherals while preserving the factory Topway phone/projection lane.

Primary target:

- Topway TS18 / UIS8581A / SP9863A family.
- Android 10 / API 29 runtime.
- 1280x720 landscape head-unit screens.
- Standard APK first.
- TS18 privileged / Magisk path only for bounded, reversible workflows with rollback.

BTAndroidTS is not a replacement for the factory Bluetooth phone app, Android Auto, ZLink/TLink, DoFun, Topway services, MCU/CAN functionality, or the Android Bluetooth stack.

## 2. Evidence Labels

Use these labels consistently in code comments, docs, PR descriptions, issue replies, and release notes:

- **Observed**: direct evidence from exact TS18 diagnostics, supplied APK/firmware/docs, or current repository code.
- **Inferred**: reasoned conclusion from observed evidence.
- **Hypothesis**: plausible but unproven explanation.
- **Requires TS18 validation**: implemented or planned but not proven on the exact TS18 device.
- **Unsupported**: outside the approved safety or authority model.

Do not treat TS18, TS10M, TS10, and other UIS8581/SP9863 units as interchangeable. Exact-device captures have highest priority.

## 3. Current Repository Shape

BTAndroidTS is a single Android app module using Clean Architecture and MVVM.

Core stack:

- Jetpack Compose and Material 3.
- Compose Destinations navigation.
- Koin dependency injection.
- Kotlin coroutines and Flow.
- Proto DataStore.
- Android Classic Bluetooth, BLE, OPP delegation, and API-gated HID Host adapters.
- Build flavors: `standard` and `ts18Privileged`.

Key paths:

- `app/src/main/java/com/cbkii/btandroidts/data/`: Android implementations, Bluetooth, BLE, OPP, diagnostics, Topway adapters, mappers, root broker implementations.
- `app/src/main/java/com/cbkii/btandroidts/domain/`: Android-light interfaces, models, policies, capability states, and business rules.
- `app/src/main/java/com/cbkii/btandroidts/di/`: dependency injection.
- `app/src/main/java/com/cbkii/btandroidts/presentation/`: Compose UI, ViewModels, navigation, state, and screen routes.
- `app/src/main/proto/`: DataStore schemas.
- `magisk/`: systemless privileged module skeleton.
- `scripts/`: validation and Magisk packaging helpers.

## 4. Exact TS18 Device Baseline

Latest exact-device baseline known to this repo:

- Device/build identity: `s9863a1h10_Natv`; `uis8581a2h10` / `sp9863a`.
- Build: `TS18.2.2_20241210.165912_WINDOW-THEME1`.
- FOTA: `WINDOW-THEME1_1000`.
- Android 10 / SDK 29; Linux `4.14.133`.
- 4 GB RAM; 64 GB-class eMMC; 1280x720 panel class.
- DoFun Variety/TWTHEME; Magisk 28.1.

Observed root/system state:

- Root capture showed `uid=0`, context `u:r:magisk:s0`.
- SELinux permissive; AVB orange/unlocked; verity enforcing.
- `/` and `/vendor` are read-only device-mapper mounts.
- `/` was around 95% used and `/vendor` around 81% used in the capture.
- Root does not provide platform keys, signature permissions, vendor identity, or UID 1000.

Observed display/storage/input constraints:

- Physical display: 1280x720 at about 58 Hz.
- App area: around 1225x720 at 153 dpi.
- Stable content area: about `[0,55]-[1225,720]`, with a 55 px top status region and right navigation region.
- Touch input reports raw 1024x600 coordinates scaled to display.
- USB volumes were observed as app-facing `/storage/usbdisk0` and `/storage/usbdisk1`, with raw root-side views under `/mnt/media_rw`.
- `com.android.externalstorage` and `com.android.providers.media` were present; `com.android.documentsui` was not found, so SAF/document picker flows may fail and direct-path/manual fallback remains important.

## 5. Bluetooth Lane Model

BTAndroidTS uses a tandem model. It improves Android peripheral management without replacing the factory automotive Bluetooth/projection lane.

### Android / Unisoc Peripheral Lane

Owner: `com.android.bluetooth`.

Observed baseline:

- Package path: `/system/app/Bluetooth`.
- Shared user: `android.uid.bluetooth` / UID 1002.
- Adapter name/address in capture: `Toparea`, `A8:82:C3:E3:E7:5A`.
- Framework declarations include GATT, OPP, HID Host, HID Device, PAN, A2DP, A2DP Sink, Headset, Headset Client, AVRCP, PBAP, MAP, and related services.
- Declaration is not proof of successful profile connection.

BTAndroidTS may work in this lane for:

- Classic discovery.
- BLE discovery and GATT inspection.
- Bonding and selective unpairing of unprotected peripherals.
- HID Host for keyboards, mice, controllers, remotes, and phone-HID apps where validated.
- Android input verification.
- OPP/file-transfer delegation where validated.

Rules for this lane:

- Use finite timeouts and observable state transitions.
- Validate MAC addresses before persistence or privileged/root operations.
- Do not edit pairing databases directly.
- Do not replace Bluetooth stack components.
- Do not assume profile declaration means connection/policy/input-node success.

### Topway Automotive Lane

Protected owner set: `com.tw.bt`, `com.tw.service*`, `com.tw.core`, `com.tw.coreservice`, `com.tw.carinfoservice`, `com.tw.eq`, DoFun/TWTHEME packages, ZLink/TLink/Gearhead integration, `gocsdk`, `blink`, `s-link`, `z-link`, and related vendor services.

Protected use:

- HFP calls.
- Phone A2DP and AVRCP.
- PBAP contacts.
- Launcher integration.
- ZLink/TLink / Android Auto / projection.
- Vendor media/audio/routing behaviour.

BTAndroidTS must treat this lane as read-only unless exact-device evidence, isolation, fallback, and rollback prove a narrow integration safe. Launch the vendor UI for phone Bluetooth settings rather than taking ownership of the phone lane.

## 6. Protected Packages and Hard Safety Boundaries

Do not disable, replace, clear data for, or casually interfere with:

- `com.tw.bt`
- `com.tw.service*`
- `com.tw.core`
- `com.tw.coreservice`
- `com.tw.carinfoservice`
- `com.tw.eq`
- `com.tw.radio`
- `com.tw.reverse`
- `com.tw.keypad`
- `com.tw.devicefan`
- `com.android.bluetooth`
- `com.android.settings`
- `com.android.providers.media`
- `com.android.externalstorage`
- `com.android.systemui`
- DoFun / TWTHEME packages
- ZLink/TLink packages
- `com.zjinnova.zlink`
- `com.google.android.projection.gearhead`
- `gocsdk`, `blink`, `s-link`, `z-link`
- FOTA/system updater packages
- Bluetooth HAL/native services such as `android.hardware.bluetooth@1.0-service.unisoc`

Unsupported actions:

- Replacing `Bluetooth.apk`, native Bluetooth libraries, HAL, WCN/controller firmware, or Topway services.
- Editing or deleting `/data/misc/bluedroid`.
- Clearing all pairings.
- Requesting `android.permission.BLUETOOTH_STACK`.
- Using `sharedUserId="android.uid.system"`.
- Claiming UID 1000, platform signing, vendor identity, or signature permission authority.
- Adding broad SELinux rules.
- Repeatedly restarting Bluetooth.
- Writing firmware, HAL, MCU, CAN, BOOT, display, or vendor partitions.
- Implementing an inbound OBEX server while stock OPP is present.
- Marking TS18 runtime behaviour passed without exact-device capture.

Say **STOP** when identity, signing, authority, firmware match, panel/BOOT match, or recovery path is inadequate.

## 7. Current Feature State

Use this table as the consolidated implementation state. “Implemented” means code or project infrastructure exists; it does not imply exact TS18 runtime validation unless stated.

| Area | Current state | Remaining work |
| --- | --- | --- |
| Product baseline | Implemented | Keep package identity, README, and release notes aligned. |
| Standard / privileged variants | Implemented | Verify privileged grant on exact TS18 before release claims. |
| Unified Bluetooth inventory | Implemented | TS18 validation for Classic/BLE population and stale expiry. |
| Bounded scans | Implemented | TS18 validation for scan reliability and OEM filtering. |
| Bonding / selective unpair | Implemented | Confirm protected-device guard and hidden API behaviour on TS18. |
| Vendor/protected device guard | Implemented | Continue adding exact-device package/name evidence only when observed. |
| Persistent supervision | Implemented | ACC sleep/wake reconnect validation still required. |
| Android 10 HID Host | Implemented behind gates | Validate connect/disconnect, policy state, and input-node creation on device. |
| Android input verification | Implemented | Validate real typing with keyboard/remote/phone-HID on TS18. |
| Phone Keyboard mode | Implemented | Validate with Android phone HID keyboard apps and TS18 host behaviour. |
| OPP/file sharing | Implemented through stock delegation | Validate picker, destination selection, progress, cancellation, and receive behaviour. |
| Safe OPP fallback posture | Implemented | Keep no inbound OBEX/private bridge unless stock path proven insufficient. |
| Topway lane preservation | Implemented in policy/docs/UI | Continue avoiding ownership of HFP/A2DP/AVRCP/PBAP/projection. |
| Capability registry | Implemented | Runtime confidence depends on TS18 validation. |
| Root broker | Partial | Keep disabled by default; only bounded diagnostics-style operations are acceptable. |
| Magisk module | Implemented skeleton/package flow | Validate install, permission grant, disable/remove rollback on device. |
| Bounded diagnostics | Implemented | Validate export paths, redaction, and USB removal on TS18. |
| Storage limits | Implemented design | Validate `/storage/usbdiskN` direct-path fallback. |
| Performance/lifecycle | Implemented design | Cold start, process death, boot, and ACC behaviour need exact-device tests. |
| Component security | Implemented posture | Recheck after every service/receiver/share-flow change. |
| CI/release validation | Implemented workflows | Do not claim local/CI checks unless exact commands were run. |

## 8. In-Car UX Direction and PR #15 Status

The legacy dashboard plan used large dashboard cards. The current development direction is a TS18 landscape-first, persistent left action rail/sidebar with a compact top app bar.

Target UX requirements:

- Replace the large multi-row dashboard button area with a persistent left sidebar/rail.
- Sidebar visible by default and collapsible to icon-only mode.
- Expanded sidebar width at or below 20% of available TS18 content width.
- Collapsed sidebar around 64-80 dp and icon-only.
- Consolidate dashboard actions and former Advanced Tools drawer entries into the sidebar.
- Use a compact `TopAppBar` rather than `MediumTopAppBar`.
- Preserve all existing actions, destinations, and scan/discovery behaviour.
- Preserve TS18 safe-zone padding for the top status region and right nav region.
- Avoid driving-state interruptions, travel warnings, modal blockers, lockouts, or motion detection.

Current active PR #15 direction:

- Replaces the large dashboard/header with `Ts18ActionSidebar`.
- Uses a master-detail `Row` layout.
- Removes `ModalNavigationDrawer` in favour of sidebar actions.
- Adds sidebar routes for Settings, About, Classic BT Server, BLE Server, Keyboard Test, Phone Keyboard, and existing TS18 actions.
- Uses compact `TopAppBar`.

Before merging/release, ensure any review cleanup has been completed:

- Remove unused sidebar parameters such as `state` and `isScanning` if not used.
- Use a reusable `SidebarItem` helper rather than repeated `NavigationRailItem` blocks.
- Use explicit expand/collapse content descriptions.
- Remove stale drawer/top-bar navigation plumbing.
- Remove dead imports and dashboard-header leftovers.
- Validate unit tests, lint, and standard debug build.

## 9. Installation, Operation, and Rollback

### Standard APK

Use the `standard` flavour for normal installation:

```bash
./gradlew assembleStandardRelease
adb install -r app/build/outputs/apk/standard/release/app-standard-release.apk
```

Runtime permissions:

- Bluetooth scan/connect/advertise where Android requires them.
- Fine location on Android 10/11 for Bluetooth scanning.

The standard APK must not request `BLUETOOTH_PRIVILEGED`, `BLUETOOTH_STACK`, UID 1000, platform signing, shared UID, SELinux policy changes, or system partition writes.

### TS18 Privileged Magisk Variant

Use only with a tested rollback path:

```bash
./gradlew assembleTs18PrivilegedRelease
bash scripts/package-magisk.sh
bash scripts/validate-magisk-package.sh com.cbkii.btandroidts build/magisk-stage/BTAndroidTS
```

The module grants only the intended narrow privileged Bluetooth permission through a privapp allowlist. It must not replace `Bluetooth.apk`, HAL libraries, firmware, Topway services, pairing databases, or platform-signed packages.

Before first boot with the module enabled:

- Preserve stock diagnostics and settings.
- Preserve known-good boot/recovery artefacts.
- Confirm Magisk module disable/remove path.
- Confirm boot-loop recovery path.

Rollback:

1. Disable the Magisk module or create the module `disable` marker.
2. Reboot.
3. Confirm `com.android.bluetooth`, `com.tw.bt`, ZLink/TLink/Android Auto, media buttons, calls, phone audio, and projection behave as stock.
4. Remove BTAndroidTS app data only after the module is disabled if a clean reset is needed.

STOP if recovery requires flashing, partition writes, Bluetooth stack replacement, Topway service disablement, clearing all bonds, or vendor pairing database edits.

## 10. Validation Matrix

Do not mark any item as passed without exact-device evidence.

| Feature area | Test case | Current status | Evidence / notes |
| --- | --- | --- | --- |
| Inventory | Classic discovery | Requires TS18 validation | Code exists; stock UI filtering remains possible. |
| Inventory | BLE discovery | Requires TS18 validation | Code exists; verify scan results on TS18. |
| Inventory | Stale record expiry | Inferred | Time-based logic and tests exist. |
| Bonding | Classic keyboard bond | Requires TS18 validation | Verify with real keyboard/remote/controller. |
| Bonding | Selective unpair | Requires TS18 validation | Must block protected Topway/vendor devices. |
| HID Host | Connection state | Requires TS18 validation | API 29 reflection path exists; permission/policy unknown on TS18. |
| HID Host | Input node creation | Requires TS18 validation | Required for real typing/pass claim. |
| Phone Keyboard | Phone HID app pairing/connect | Requires TS18 validation | Dedicated workflow exists. |
| Input UX | Keyboard Test screen | Requires TS18 validation | UI exists; verify actual input events. |
| OPP | Share single file/text/URL | Requires TS18 validation | Delegated to stock Bluetooth OPP. |
| OPP | History persistence | Inferred / implemented | DataStore-backed app history exists. |
| Topway lane | Lane identification | Observed / implemented | Package names and guardrails exist. |
| Topway lane | Projection coexistence | Requires TS18 validation | Must validate ZLink/TLink/Android Auto after BTAndroidTS changes. |
| Supervision | Backoff retry | Inferred / implemented | Alarm/retry logic exists. |
| Supervision | ACC wake reconnect | Requires TS18 validation | Real vehicle sleep/wake required. |
| Diagnostics | Local export | Requires TS18 validation | Validate internal and `/storage/usbdiskN` paths. |
| Diagnostics | Redaction | Inferred / implemented | Redaction logic and tests exist. |
| Magisk | Privileged permission grant | Requires TS18 validation | Install path exists; grant must be captured. |
| Magisk | Disable/remove rollback | Requires TS18 validation | Must prove recovery before release claims. |
| UI | Sidebar/compact top bar | Requires TS18 validation after merge | Check 1280x720, 1225x665 stable content, touch usability. |

## 11. Release-Readiness Checklist

Before release, record exact command output or CI links for:

```bash
./gradlew testStandardDebugUnitTest
./gradlew lintStandardDebug
./gradlew assembleStandardDebug
./gradlew assembleStandardRelease
./gradlew assembleTs18PrivilegedRelease
sh scripts/check-manifest-permissions.sh
sh scripts/package-magisk.sh
sh scripts/validate-magisk-package.sh
```

Release notes must state:

- Which build/flavour was produced.
- Whether Magisk packaging was built and validated.
- Whether exact TS18 runtime validation was performed.
- Which items still require TS18 validation.
- How to roll back.

## 12. Documentation Hygiene

Keep docs minimal and current:

- `README.md`: user-facing overview, installation, usage, screenshots, and safety summary.
- `AGENTS.md`: hard repository rules, architecture constraints, TS18 safety boundaries, and agent guidance.
- This file: consolidated technical/evidence/status/validation/operation/rollback/roadmap reference.
- Optional `docs/ROADMAP.md`: short active work queue if it adds value; otherwise keep roadmap here.

Safe cleanup candidates after this file is committed and reviewed:

- `docs/TS18_DEVICE_CONTEXT.md`
- `docs/BLUETOOTH_LANE_MODEL.md`
- `docs/VALIDATION_MATRIX.md`
- `docs/INSTALLATION_OPERATION_ROLLBACK.md`
- `docs/IMPLEMENTATION_STATUS.md`

Do not commit generated PR exports, prompt transcripts, bot review dumps, large third-party repository snapshots, CI logs, or zipped research packs unless a task explicitly requires a small curated reference.
