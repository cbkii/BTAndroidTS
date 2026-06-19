# TS18 Device Context

This document separates exact-device evidence from broader TS18 precedent and implementation
assumptions. A pass claim here means the evidence item itself was present in the exact-device
capture. It does not mean a new BTAndroidTS feature has been run on the head unit.

## Observed

- The local-only diagnostic run completed locally on the target device and finished at `2026-06-18T15:16:36Z`.
- The exact target is `s9863a1h10_Natv`, board/hardware `uis8581a2h10` / `sp9863a`, build
  `TS18.2.2_20241210.165912_WINDOW-THEME1`, Android 10 / SDK 29, Linux `4.14.133`, 4 GB RAM,
  and 1280x720 panel class.
- Root state is Magisk 28.1 with `uid=0(root)` and SELinux context `u:r:magisk:s0`. The boot state
  is AVB orange/unlocked, SELinux permissive, and verity enforcing. No Magisk modules were installed
  in the capture; only bounded diagnostic scripts existed.
- `/` and `/vendor` are read-only device-mapper mounts. `/` was about 95% used and `/vendor` about
  81% used, so direct partition writes, large extraction, and unbounded logging are unsafe defaults.
- Two FAT USB volumes were mounted as app-facing `/storage/usbdisk0` and `/storage/usbdisk1`, with
  raw root-side views under `/mnt/media_rw`. `com.android.externalstorage` and
  `com.android.providers.media` were present; `com.android.documentsui` was not found.
- Display/window evidence shows physical 1280x720 at about 58 Hz, app area 1225x720 at 153 dpi, a
  stable content area of `[0,55]-[1225,720]`, a 55 px status bar, and a right navigation bar.
  Touch input reports raw 1024x600 coordinates scaled to the display.
- Android Bluetooth manager evidence shows adapter name `Toparea`, address `A8:82:C3:E3:E7:5A`,
  zero stack crashes, state `BLE_ON`, no active GATT clients/servers, and one bonded LE device:
  `SmartRemote` at `DE:8F:7D:8E:A3:1E`. This proves baseline stack availability and bond state,
  not HID input success.
- `com.android.bluetooth` is installed as `/system/app/Bluetooth`, shared user
  `android.uid.bluetooth` / UID 1002, target SDK 29. Package/service resolver evidence declares
  GATT, OPP, HID Host, HID Device, PAN, A2DP, A2DP Sink, Headset, Headset Client, AVRCP, PBAP, MAP,
  and related Bluetooth services.
- `com.tw.bt` is a privileged persistent system package under `/system/priv-app`, shared user
  `android.uid.system` / UID 1000, with boot/ACC receivers and phone/Bluetooth permissions. This is
  the protected Topway phone lane, not an app identity BTAndroidTS can impersonate.
- `com.zjinnova.zlink` is a normal data app. It requests privileged Bluetooth and phone-state
  permissions, but package evidence shows it is not a privileged UID 1000 package. Vendor properties
  still identify it as the phone-connect app and expose a Topway/ZLink Bluetooth address
  `00:87:61:A9:26:26`.
- Running process/service evidence includes `android.hardware.bluetooth@1.0-service.unisoc`,
  `com.android.bluetooth`, `com.tw.bt`, `com.tw.service`, `com.tw.eq`, DoFun, Gearhead,
  `com.zjinnova.zlink`, and `com.tw.media`. Media-session evidence shows `com.tw.media` owns the
  media button session in the captured state.
- Crash folders, tombstones, pstore, ANR, and dropbox evidence were empty in the capture. Memory was
  normal with about 1.7 GB free RAM, but the system already had large resident OEM/projection/media
  processes.
- The OEM PDF identifies the TS18/Toparea UIS8581A Android 10 board family, documents USB Host vs
  USB OTG separation, and states built-in Bluetooth 5.0 support for phonebook, A2DP, Bluetooth OBD,
  mouse, gamepad, keyboard, and other external Bluetooth devices. These are board/vendor support
  statements, not a runtime pass for this firmware.

## Inferred

- The first-pass package split is compatible with this device baseline: `standard` keeps to normal
  Android Bluetooth permissions, while `ts18Privileged` isolates `BLUETOOTH_PRIVILEGED` to the build
  variant that would require systemless privileged placement and grant verification.
- Android 10 / API 29 as `minSdk` matches the captured runtime. Targeting a newer SDK is acceptable
  only while every newer API, permission rule, and foreground-service behavior is runtime-gated.
- The Android/Unisoc Bluetooth lane is suitable for standards-based peripheral management, provided
  BTAndroidTS does not replace `Bluetooth.apk`, HAL, firmware, pairing databases, or Topway services.
- The Topway/ZLink lane likely owns phone HFP, A2DP, AVRCP, PBAP, media buttons, launcher/projection
  flows, and some hardware routing. BTAndroidTS must not claim phone-profile ownership by default.
- UI work should use runtime bounds/insets and clamp saved positions. Full 1280x720 assumptions risk
  overlap with the status bar, right navigation bar, and OEM overlays.
- Storage/export work should prefer `/storage/usbdiskN` and include a manual/direct-path fallback
  because DocumentsUI is absent even though external storage and media providers exist.
- MAC addresses may be randomised.

## Hypothesis

- Stock Settings or picker flows may filter devices even when framework discovery receives events.
- HID Host and OPP are present as framework services, but connection policy, permission gating,
  picker behavior, and input-node creation still need exact-device runtime evidence.
- A privileged OPP or HID helper may be useful only if the normal Android path is demonstrably
  insufficient and the helper remains systemless, capability-gated, and reversible.

## Requires Device Validation

- Classic keyboard HID connection and typing.
- BLE/HOGP peripheral input-node creation.
- `SmartRemote` input-node creation.
- Phone HID keyboard operation while the same phone remains paired to the Topway lane.
- Cold-boot and ACC sleep/wake HID reconnection.
- TLink/ZLink operation while Android Bluetooth remains on.
- Magisk priv-app permission grant on the exact firmware.
- Boot-loop rollback and module-disable procedure after a privileged Magisk package is introduced.
- External USB export through both observed `usbdiskN` paths with media removal.

## Unsupported

- Replacing `Bluetooth.apk`, native Bluetooth libraries, HAL, or firmware.
- Claiming platform signing, UID 1000, or `android.permission.BLUETOOTH_STACK`.
- Editing pairing databases directly.
- Clearing all bonds.
- Disabling Topway phone/projection services to make Android Bluetooth work.
- Treating the OEM PDF as permission to flash, ground key wires, or use recovery files without an
  exact file set, port, power sequence, and rollback plan.

## Local Evidence Reviewed In Second Pass

- `docs/TS18diagnostics/SUMMARY.txt`
- `docs/TS18diagnostics/identity/*`
- `docs/TS18diagnostics/magisk/*`
- `docs/TS18diagnostics/storage/*`
- `docs/TS18diagnostics/display/*`
- `docs/TS18diagnostics/input/*`
- `docs/TS18diagnostics/network/bluetooth-manager.txt`
- `docs/TS18diagnostics/android/package-full.txt`
- `docs/TS18diagnostics/packages/inspect/*`
- `docs/TS18diagnostics/vendor/*`
- `docs/TS18diagnostics/media/*`
- `docs/TS18diagnostics/power/*`
- `docs/TS18diagnostics/memory/*`
- `docs/TS18diagnostics/crashes/*`
- `docs/TS18diagnostics/DEBLOAT_REVIEW.txt`
- `docs/TS18-OEM_docs.pdf`
