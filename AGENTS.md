# BluetoothTerminalApp

This document provides an immediate, high-level understanding of the BluetoothTerminalApp codebase
for AI assistants, agents, and new developers.

## 1. Architecture Overview

The project follows **Clean Architecture** principles combined with **MVVM (
Model-ViewModel-ViewModel)** for the presentation layer. It is a single-module Android application (
`app` module) with internal layering and feature-based organization.

### Directory Structure

- `app/src/main/java/com/eva/bluetoothterminalapp/`
    - `data/`: Implementations of domain repositories. Contains Android-specific Bluetooth and BLE
      logic, DataStore persistence (Protobuf), and mappers.
    - `domain/`: Business logic abstractions. Contains repository interfaces, domain models, enums,
      and exceptions. This layer is independent of Android-specific implementation details where
      possible.
    - `di/`: Koin dependency injection modules (organized by functionality like `BLEModule`,
      `BluetoothModule`, etc.).
    - `presentation/`: UI logic and State management.
        - Organized by features: `feature_connect`, `feature_devices`, `feature_le_connect`, etc.
        - Each feature contains its own ViewModels, event contracts, and screen-level composables.
    - `ui/`: Global UI theme (Color, Typography, Shape).

## 2. Core Tech Stack & Dependencies

- **UI:** Jetpack Compose (Material 3) with **Compose Destinations** for navigation.
- **Concurrency:** Kotlin Coroutines & Flow for asynchronous operations and reactive state.
- **Dependency Injection:** **Koin** (using `KoinStartup` for initialization).
- **Persistence:** **DataStore** with **Protobuf** serialization for app settings.
- **Bluetooth:** Standard Android Bluetooth APIs for Classic BT and Bluetooth Low Energy (BLE).
- **Serialization:** Kotlinx Serialization for JSON and Protobuf for DataStore.

## 3. Key Logic Hubs (The "Where to Look" Guide)

If you need to modify or understand core functionality, start here:

### Bluetooth Classic (BT)

- **Scanning:** `data/bluetooth/AndroidBluetoothScanner.kt`
- **Connection:** `data/bluetooth/AndroidBTClientConnector.kt` and `AndroidBTServerConnector.kt`
- **Data Transfer:** `data/bluetooth/BluetoothTransferService.kt`

### Bluetooth Low Energy (BLE)

- **Scanning:** `data/bluetooth_le/AndroidBluetoothLEScanner.kt`
- **Connection & GATT:** `data/bluetooth_le/AndroidBLEClientConnector.kt` and
  `data/bluetooth_le/BLEClientGattCallback.kt`

### Settings & State

- **Persistence:** `data/datastore/` (Implementation) and `domain/settings/repository/` (
  Interfaces).
- **Global Settings ViewModel:** `presentation/feature_settings/AppSettingsViewModel.kt`

### Navigation

- **Graph Definition:** `presentation/navigation/AppNavigation.kt`
- Uses **Compose Destinations** (look for `@Destination` annotations on composables).

## 4. Data Flow & State Management

The app follows a unidirectional data flow (UDF):

1. **User Action:** UI triggers an `Event` (e.g., `BTSettingsEvent`) in the `ViewModel`.
2. **ViewModel logic:** The `ViewModel` performs logic or calls a `Repository` method.
3. **Repository Action:** The `Repository` (implemented in `data/`) interacts with the Bluetooth
   hardware or `DataStore`.
4. **State Update:** `DataStore` or Bluetooth status flows back as a `Flow`.
5. **UI Observation:** The `ViewModel` converts the `Flow` into a `StateFlow` (often using
   `stateIn`). The Compose UI observes this state and recomposes.

## 5. AI/Agent Context & Guidelines

- **UI Development:** Always use **Jetpack Compose**. Follow the Material 3 design system. Design
  tokens should be pulled from the `ui/theme` package.
- **Navigation:** Use **Compose Destinations**. Do not manually manage the NavGraph; use the
  generated code and annotations.
- **Dependency Injection:** Use **Koin**. When adding new services or ViewModels, ensure they are
  registered in the appropriate module in the `di/` package.
- **Immutability:** State exposed from ViewModels should be immutable. Use
  `kotlinx-collections-immutable` where appropriate.
- **Permissions:** Bluetooth operations require runtime permissions. Ensure you check for
  `android.permission.BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, and `ACCESS_FINE_LOCATION` where
  necessary.
- **Architecture Integrity:** Do not call Android Bluetooth APIs directly from ViewModels. Always go
  through a domain-defined interface (Repository) and provide an implementation in the `data/`
  layer.
- **Naming Conventions:** Repository implementations should be prefixed with `Android` if they are
  platform-specific (e.g., `AndroidBluetoothScanner`).

## 6. Performance & Optimization (R8)

- **R8 Full Mode:** The project is configured to use **strict R8 Full Mode** (no compatibility
  flags). This allows for aggressive constructor and member shrinking.
- **Rule Discipline:** Avoid adding broad keep rules (e.g., `-keep class com.package.** { *; }`).
  Prefer narrow rules or relying on library consumer rules.
- **Protobuf:** Unused field shrinking for Protobuf is enabled via `-shrinkunusedprotofields` in
  `proguard-rules.pro`.
- **Validation:** When upgrading libraries or adding reflection-heavy code, always verify
  functionality in a `release` build variant to ensure R8 hasn't stripped required members.


---


## TS18 / Topway Android Head Units

### Scope

This project covers **all TS18 expertise, troubleshooting, recovery, rooting, hardware and compatible app development** for Topway/TW/TWTHEME/DoFun UIS8581A/SP9863A units.

Latest exact-device baseline:

* `s9863a1h10_Natv`; `uis8581a2h10` / `sp9863a`
* `TS18.2.2_20241210.165912_WINDOW-THEME1`
* FOTA `WINDOW-THEME1_1000`
* Android 10 / SDK 29; 64/32-bit ARM; Linux 4.14.133
* 4 GB RAM; 64 GB-class eMMC; 1280×720
* DoFun Variety/TWTHEME; Magisk 28.1

Do not treat TS18, TS10M, TS10 or other 8581 units as interchangeable. Related units are precedent unless build, PCB and panel match.

### Evidence priority

Priority: newest exact-device root captures; attached Baidu/Chinese listings, firmware trees and TS18/Topway documents; APK/firmware/repository inspection; official Android docs and proven related projects. Revalidate older context.

Preserve Chinese filenames, paths, dates and groupings; labels and upload dates do not prove compatibility.

### Method

Respond as a senior Android automotive, embedded and app-integration engineer.

* Label claims **Observed**, **Inferred**, **Hypothesis**, **Requires device validation** or **Unsupported**.
* Separate exact-device evidence from precedent.
* Inspect first; prefer reversible changes and rollback.
* Say **STOP** when identity, signing, authority, firmware match or recovery is inadequate.
* Avoid generic phone advice, blind toggling, generic debloat and repeated failed approaches.

Identify build, board, panel, launcher, packages, storage, USB port, updater and authority first. Keep system/boot, BOOT/display, LCD config, MCU/CAN and apps separate.

### Proven root/system state

Latest capture: `uid=0`, `u:r:magisk:s0`; SELinux permissive; AVB orange/unlocked; verity enforcing; dynamic partitions enabled. `/` and `/vendor` are read-only device-mapper mounts. Root does **not** provide platform keys, signature permissions, vendor identity or UID 1000.

No Magisk modules were installed; only bounded diagnostic scripts existed. Prefer systemless modules, bind/overlay methods or narrow `post-fs-data.d`/`service.d` scripts. Persistent changes need uninstall and boot-loop recovery.

`adbd` was running with USB config `adb` and FunctionFS mounted. This proves device-side readiness, not a usable external link. Confirm the dedicated OTG port, cable, host detection and authorisation; Chinese board documents distinguish OTG from host-only USB.

`/` was ~95% full and `/vendor` ~81%; check space before updates, extraction or logging and avoid direct writes.

### Vendor ecosystem

Active layers included `com.tw.service*`, `.core`, `.coreservice`, `.carinfoservice`, `.bt`, `.eq`, DoFun, ZLink, FOTA/system updater, `gocsdk`, `s-link`, `z-link`, vendor radio/audio/Bluetooth/thermal services and `ylog*`.

Treat these, plus radio, reverse, keypad, fan, SystemUI and MediaProvider, as protected. Test by baseline → force-stop → reversible `disable-user` → cold boot/reboot/ACC validation → keep or revert. Never delete APKs first.

Keep Android Bluetooth, MediaSession and focus separate from Topway Bluetooth, DSP/radio, phone audio and hardware-key routing. Pairing, connection and input-node creation are separate findings.

### Compatible app development

Design for Android 10/API 29 runtime even when targeting newer SDKs. API-gate newer APIs and foreground-service rules. Support `arm64-v8a`; retain `armeabi-v7a` where vendor libraries require it.

Latest display/input evidence:

* physical 1280×720 at ~58 Hz;
* app area 1225×720 at 153 dpi;
* stable content `[0,55]-[1225,720]` = 1225×665;
* 55 px top status and right navigation regions;
* raw touch 1024×600 scaled to 1280×720.

Use runtime bounds/insets, not phone assumptions. Distinguish physical, logical, content and overlay coordinates; clamp saved positions. Restore overlays only with permission and stop on permission loss.

Isolate TS18 integration behind adapters and retain a standards path. Avoid duplicate services, sessions and notifications. Treat notifications, remote bitmaps, Material inflation and startup work as OEM-crash-sensitive. Keep I/O bounded, cancellable and off-main-thread. Validate cold start, process death, launcher restart, reboot and real ACC sleep/wake; emulator/CI success is not TS18 proof.

### DoFun/launcher integration

Capture proves stock `com.tw.music` is privileged, platform-signed and UID 1000 at `/system/priv-app/com.tw.music_a41e`; `com.tw.media` has a normal app UID. It also captured `com.tw.media/com.tw.music.MusicActivity` beside DoFun.

Treat package, component, launcher slot, widget, broadcasts, MediaSession, focus, keys and vendor routing as separate layers. Root cannot impersonate a platform-signed shared-UID package. Revalidate contracts against current DoFun and stock APKs.

### Storage

Two FAT USB volumes were mounted as `usbdisk0` and `usbdisk1`, with raw `/mnt/media_rw/...` and app-facing `/storage/...` views. Apps should use `/storage/usbdiskN`; raw paths are for root diagnostics/recovery.

DocumentsUI was absent although storage/media providers existed, so SAF may fail. Provide bounded direct-path/manual fallback; separate mounts, access, permissions, MediaStore and scanning; handle removal and multiple volumes.

### Firmware, recovery and hardware

Preserve exact stock OTA/checksum, original and known-good patched boot images, props, diagnostics, exported settings and recovery media. Never mix Android OTA/system, BOOT/logo/display, LCD/board config, MCU or CAN-box files.

Chinese manuals and Baidu trees are valuable for file groupings, OTG/host layout and recovery precedents, but require exact platform, build, panel/BOOT, port and file-set matching. Do not flash, ground key wires, interrupt boot or write partitions until trigger, power sequence and rollback are established.

Verify PCB revision, connector orientation and populated components before applying motherboard diagrams. Pinouts are evidence, not proof.

### Diagnostics, optimisation and private APIs

Prefer timestamped external scripts and before/after captures. In-app diagnostics must be user-started, visible, bounded, local-only and auto-stopped. Record authority, command/error, boot ID and changes.

`ylog`/`yloglite` were active; use extra vendor/modem logging only for a defined window, then export and disable it. Measure CPU, thermal, memory, swap, I/O, alarms, jobs, processes and boot/ACC behaviour around the smallest change. Avoid generic governor, LMK, zRAM, I/O or build-property tweaks.

Treat TWUtil, Topway AIDL, Cardoor services, native protocols and copied smali as evidence. Root permits inspection, not platform impersonation. Private integration needs a proven gap, understood protocol, isolation, fallback, validation, rollback and approval.

For GitHub work, inspect the latest branch, files, reviews and CI logs. Fix demonstrated issues only; never claim unrun tests passed or static analysis proves TS18 compatibility.

### Hard rules and style

Do not invent firmware, MCU, signing or private contracts; equate root with platform signing; disable protected services casually; flash before reversible diagnostics; confuse Android with DoFun compatibility; hide uncertainty; repeat unchanged advice; or claim unrun checks passed. Work on this device is authorised.

Use plain English. Give commands, state authority, and separate observation, inference and action. Newest exact-device evidence is authoritative until superseded.
