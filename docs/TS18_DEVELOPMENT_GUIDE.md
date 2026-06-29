# BTAndroidTS TS18 Development Guide

This is the compact development reference for BTAndroidTS. Keep user-facing install/use notes in `README.md`, hard repository rules in `AGENTS.md`, and planned/incomplete work in `docs/ROADMAP.md`.

## Purpose

BTAndroidTS is a Bluetooth peripheral manager for Topway TS18 Android head units. It manages Android Bluetooth peripherals while preserving the factory Topway phone/projection lane.

Primary runtime target:

- Topway TS18 / UIS8581A / SP9863A family.
- Android 10 / API 29 runtime.
- 1280x720 landscape head-unit display.
- Standard APK first; Magisk privileged variant only for bounded, reversible TS18 workflows.

## Architecture

Single Android app module using Jetpack Compose, Material 3, Compose Destinations, Koin, Kotlin coroutines/Flow, Proto DataStore, Classic Bluetooth, BLE, OPP delegation, and narrow API-gated privileged adapters.

Key paths:

- `app/src/main/java/com/cbkii/btandroidts/data/`: Android implementations, Bluetooth/BLE/OPP, diagnostics, mappers, and Topway adapters.
- `app/src/main/java/com/cbkii/btandroidts/domain/`: Android-light interfaces, models, policies, and business rules.
- `app/src/main/java/com/cbkii/btandroidts/presentation/`: Compose UI, ViewModels, navigation, and screen state.
- `app/src/main/proto/`: DataStore schemas.
- `magisk/`: systemless privileged module skeleton.
- `scripts/`: validation and Magisk packaging helpers.

## Evidence Labels

Use these labels consistently:

- **Observed**: exact TS18 capture, supplied diagnostic, APK/firmware inspection, or current repository code.
- **Inferred**: reasoned from observed evidence.
- **Hypothesis**: plausible but unproven.
- **Requires device validation**: implemented or planned but not proven on the exact TS18.
- **Unsupported**: outside the approved safety model.

## Exact TS18 Baseline

Latest known exact-device baseline:

- `s9863a1h10_Natv`; `uis8581a2h10` / `sp9863a`.
- `TS18.2.2_20241210.165912_WINDOW-THEME1`.
- FOTA `WINDOW-THEME1_1000`.
- Android 10 / SDK 29; Linux 4.14.133.
- 4 GB RAM; 64 GB-class eMMC; 1280x720.
- DoFun Variety/TWTHEME; Magisk 28.1.

Root state to remember:

- Observed `uid=0`, `u:r:magisk:s0` when rooted.
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

Keep Android Bluetooth, MediaSession, audio focus, phone audio, contacts, projection, and Topway Bluetooth as separate lanes.

## Hard Safety Boundaries

Do not:

- replace `Bluetooth.apk`, Bluetooth HAL, native libraries, WCN/controller firmware, or Topway services;
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

Say **STOP** when identity, signing, authority, firmware match, panel/BOOT match, or recovery path is inadequate.

## Current Development Focus

Keep the TS18 Bluetooth devices screen focused on in-car glanceability:

- Compact top app bar.
- Persistent collapsible left action sidebar/rail for primary actions and advanced tools.
- Main device list/tabs get the remaining space.
- No driving-state interruptions, warnings, lockouts, motion detection, or blocking overlays.
- Preserve Android 10/API 29 compatibility and TS18 safe-zone handling.

## Validation Commands

Run from an Android/Linux shell when changing behavior:

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

If a command cannot run, record the exact command and exact failure. Do not claim unrun checks passed.
