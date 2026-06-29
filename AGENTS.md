# BTAndroidTS Agent Guide

This file is for developers, reviewers, contributors, and AI agents working in this repository.
The public README is for end users; keep implementation details, validation rules, and agent
instructions here or under `docs/`.

## Project Shape

BTAndroidTS is a single-module Android app using Clean Architecture and MVVM:

- `app/src/main/java/com/cbkii/btandroidts/data/`: Android-specific implementations, Bluetooth
  APIs, DataStore, OPP, diagnostics, Topway adapters, and platform mappers.
- `app/src/main/java/com/cbkii/btandroidts/domain/`: Android-light interfaces, models, policies,
  capability states, and business rules.
- `app/src/main/java/com/cbkii/btandroidts/di/`: Koin dependency injection modules.
- `app/src/main/java/com/cbkii/btandroidts/presentation/`: ViewModels, Compose UI, navigation, and
  screen state.
- `app/src/main/proto/`: Protobuf DataStore schemas.
- `docs/`: TS18 evidence, implementation status, validation matrix, rollback, and lane model.
- `magisk/`: Systemless privileged module skeleton.
- `scripts/`: Android/Linux shell validation and packaging scripts.

## Core Stack

- UI: Jetpack Compose and Material 3.
- Navigation: Compose Destinations.
- Concurrency: Kotlin Coroutines and Flow.
- Dependency injection: Koin.
- Persistence: DataStore with Protobuf.
- Bluetooth: Android Classic Bluetooth, BLE, OPP delegation, and API-gated HID Host adapters.
- Build variants: `standard` and `ts18Privileged`.

## Development Rules

- Use Android/Linux shell commands and `scripts/*.sh` in documentation. Do not add alternative host
  shell guidance.
- Keep Android framework APIs out of ViewModels. Use domain interfaces and inject data-layer
  implementations.
- Keep hidden/reflected APIs isolated behind narrow capability interfaces.
- Add narrow R8 keep rules only for reflected members that are actually looked up.
- Prefer DataStore or existing persistence patterns for durable state.
- Validate MAC addresses before persistence or privileged/root operations.
- Keep I/O bounded, cancellable, and off the main thread.
- Do not add broad services, receivers, foreground notifications, boot work, or root commands.
- Do not claim unrun tests, lint, CI, or TS18 runtime checks passed.

## Key Logic Hubs

Classic Bluetooth:

- `data/bluetooth/AndroidBluetoothScanner.kt`
- `data/bluetooth/AndroidBTClientConnector.kt`
- `data/bluetooth/AndroidBTServerConnector.kt`
- `data/bluetooth/BluetoothTransferService.kt`

BLE:

- `data/bluetooth_le/AndroidBluetoothLEScanner.kt`
- `data/bluetooth_le/AndroidBLEClientConnector.kt`
- `data/bluetooth_le/BLEClientGattCallback.kt`

TS18 peripheral manager:

- `domain/peripheral/*`
- `data/peripheral/*`
- `data/opp/*`
- `presentation/feature_devices/*`
- `presentation/feature_opp/*`
- `app/src/main/proto/peripheral_policy.proto`

## Required Validation Commands

Run these from an Android/Linux shell when changing behavior:

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

If a command cannot run, record the exact command and exact failure.

## TS18 Evidence Rules

Use these labels consistently:

- Observed: direct evidence from the exact TS18, supplied diagnostics, docs, APK inspection, or
  repository code.
- Inferred: reasoned conclusion from observed evidence.
- Hypothesis: plausible but unproven explanation.
- Requires device validation: implementation may exist, but no exact TS18 runtime pass exists.
- Unsupported: outside the approved safety model.

Do not treat TS18, TS10M, TS10, or other 8581 units as interchangeable. Exact-device captures are
highest priority.

## Exact-Device Baseline

Latest exact-device baseline:

- `s9863a1h10_Natv`; `uis8581a2h10` / `sp9863a`.
- `TS18.2.2_20241210.165912_WINDOW-THEME1`.
- FOTA `WINDOW-THEME1_1000`.
- Android 10 / SDK 29; Linux 4.14.133.
- 4 GB RAM; 64 GB-class eMMC; 1280x720.
- DoFun Variety/TWTHEME; Magisk 28.1.

Root state:

- Observed `uid=0`, `u:r:magisk:s0`.
- SELinux permissive; AVB orange/unlocked; verity enforcing.
- `/` and `/vendor` are read-only device-mapper mounts.
- Root does not provide platform keys, signature permissions, vendor identity, or UID 1000.

## Protected Vendor Ecosystem

Treat these as protected unless exact-device evidence and a rollback plan prove otherwise:

- `com.tw.bt`
- `com.tw.service*`
- `com.tw.core`
- `com.tw.coreservice`
- `com.tw.carinfoservice`
- `com.tw.eq`
- DoFun / TWTHEME packages
- ZLink/TLink packages
- `gocsdk`, `blink`, `s-link`, `z-link`
- vendor radio, audio, Bluetooth, thermal, reverse, keypad, and fan services
- `com.android.bluetooth`
- SystemUI, MediaProvider, external storage providers, FOTA/system updater

Keep Android Bluetooth, MediaSession, audio focus, phone audio, contacts, projection, and Topway
Bluetooth as separate lanes.

## Hard Safety Boundaries

Do not:

- replace `Bluetooth.apk`, Bluetooth HAL, native libraries, WCN/controller firmware, or Topway
  services;
- edit or delete `/data/misc/bluedroid`;
- clear all pairings;
- disable protected Topway, projection, media, SystemUI, updater, or Bluetooth packages;
- request `android.permission.BLUETOOTH_STACK`;
- use `sharedUserId="android.uid.system"`;
- claim UID 1000, platform signing, or vendor identity;
- add broad SELinux rules;
- repeatedly restart Bluetooth;
- write to firmware, HAL, MCU, CAN, BOOT, display, or vendor partitions;
- implement an inbound OBEX server while stock OPP is present;
- mark TS18 runtime behavior as passed without exact-device capture.

Say STOP when identity, signing, authority, firmware match, panel/BOOT match, or recovery path is
inadequate.

## Documentation Split

- `README.md`: end-user overview, install choices, app usage, and safety summary.
- `CONTRIBUTING.md`: contributor workflow if present.
- `AGENTS.md`: repository rules, architecture constraints, TS18 safety boundaries, and AI-agent guidance.
- `docs/TS18_DEVELOPMENT_GUIDE.md`: consolidated technical/evidence/status/validation/rollback/lane reference.
- `docs/ROADMAP.md`: planned and incomplete work.

Keep this split when editing Markdown. Do not commit generated review exports, prompt transcripts, large third-party source snapshots, CI logs, or zipped research packs unless a future task explicitly requires a small curated reference.
