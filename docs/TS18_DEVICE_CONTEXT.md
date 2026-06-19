# TS18 Device Context

This document separates exact-device evidence from broader TS18 precedent and implementation
assumptions.

## Observed

- The available local request material identifies the primary target as UIS8581A / SP9863A,
  model `s9863a1h10_Natv`, Android 10, runtime API 29.
- The prompt states the captured build identity as `TS18.2.2_20241210.165912_WINDOW-THEME1`.
- The prompt states Magisk 28.1 root, unlocked AVB/orange state, and read-only dynamic
  `/system` and `/vendor` device-mapper mounts.
- The latest AGENTS excerpt states `uid=0`, `u:r:magisk:s0`, SELinux permissive, verity enforcing,
  and no installed Magisk modules in the latest capture.
- The latest AGENTS excerpt states `/` was approximately 95% full and `/vendor` approximately 81%
  full; avoid direct partition writes, large local extraction, or unbounded logging.
- The latest AGENTS excerpt states a physical 1280x720 display at approximately 58 Hz, app area
  1225x720 at 153 dpi, stable content `[0,55]-[1225,720]`, and 55 px top/right system regions.
- The latest AGENTS excerpt states FAT USB volumes were observed as app-facing `/storage/usbdisk0`
  and `/storage/usbdisk1`, while DocumentsUI was absent.
- The prompt states an Android/Unisoc adapter name `Toparea` and observed address
  `A8:82:C3:E3:E7:5A`.
- The prompt states a Topway controller name `CarKit_blink` and observed address
  `00:87:61:A9:26:26`.
- The prompt states an existing `SmartRemote` peripheral at `DE:8F:7D:8E:A3:1E`, bonded, with
  observed HID service UUID `0x1812`.

## Inferred

- The Android/Unisoc lane can be used for peripheral management without replacing either Bluetooth
  stack.
- The Topway automotive lane likely owns phone HFP, A2DP, AVRCP, PBAP, launcher integration, and
  TLink/ZLink projection flows.
- A privileged app installed systemlessly through Magisk is the least invasive way to request
  Android privileged Bluetooth operations on this rooted device.
- UI should use runtime window bounds and insets rather than phone portrait assumptions or fixed
  full-screen coordinates.
- File and diagnostics export should prefer app-facing `/storage/usbdiskN` paths when a manual USB
  target is needed, and must handle absent SAF/DocumentsUI.

## Hypothesis

- Some stock Settings or picker flows may filter or fail to render devices even when framework
  discovery receives events.
- HID Host capability exists in the framework services but still requires real-device validation
  for profile policy and input-node creation.
- OPP outbound selected-device delegation may require a version-gated bridge if the stock picker is
  unusable.

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

## Evidence Availability

The task brief requested inspection of `BT-HID-SEPARATE-GUIDE.md`, `TS18_diagnostics.zip`,
`ts18-sh.zip`, `TS18-props.zip`, and `combined-small_en-US.pdf`. Those files were not present in
the current workspace, `Downloads`, or visible Codex attachment directory during this implementation
pass. Claims above therefore come from the pasted brief, `TS18.md`, and the latest AGENTS excerpt;
they are not represented as fresh archive revalidation.
