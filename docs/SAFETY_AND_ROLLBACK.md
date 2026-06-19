# Safety and Rollback

## Non-Negotiable Safety Rules

BTAndroidTS must not:

- replace `Bluetooth.apk`;
- replace native Bluetooth libraries, HAL, or controller firmware;
- add speculative Android phone-profile overlays;
- edit `/data/misc/bluedroid` or vendor pairing databases directly;
- clear all bonds;
- continuously restart Bluetooth;
- disable Topway services;
- request `android.permission.BLUETOOTH_STACK`;
- claim `android.uid.system`, platform signing, or shared UID;
- add SELinux policy without explicit validation and approval.
- write directly to `/system`, `/vendor`, Android OTA/system partitions, BOOT/logo/display assets,
  LCD/board config, MCU, or CAN-box files.
- perform large diagnostics, extraction, or logging on nearly full root/vendor partitions.

## Root Helper Constraints

Any root helper must be secondary to Android API and privileged API paths. It must:

- expose only fixed allowlisted operations;
- validate MAC addresses before use;
- use fixed command structures and typed arguments;
- apply finite timeouts;
- capture operation name, authority, timing, exit status, stdout, and stderr;
- verify state after execution;
- default off in the standard build;
- never edit pairing databases or write `/system` or `/vendor` directly.

## Magisk Rollback Model

The privileged deployment should be systemless:

1. Disable the BTAndroidTS Magisk module in Magisk Manager, or create a module `disable` marker.
2. Reboot.
3. Verify the stock package and Topway lane are still present.
4. Uninstall BTAndroidTS app data only after the module is disabled if a clean reset is needed.

No rollback step should require deleting Bluetooth stack data.

Before enabling a module on the exact TS18, preserve stock OTA/checksum evidence, original and
known-good patched boot images, exported settings, diagnostics, and recovery media. Confirm there is
a known disable path for the module before adding boot-time scripts.

## STOP Conditions

Stop the affected path and document the blocker if:

- Android Bluetooth ON breaks Topway HFP, phone audio, or projection.
- HID requires replacing stack APKs, HAL libraries, or controller firmware.
- privileged permission is not granted despite correct systemless placement.
- a hidden API call fails and no narrow, verified fallback exists.
- OPP integration requires disabling the stock incoming service.
- package signing changes would block future upgrades.
- a service causes boot loops, repeated stack restarts, or notification crashes.
- identity, board, build, panel/BOOT, signing authority, or recovery path is inconsistent.
- the only validation path requires flashing, grounding key wires, interrupting boot, or mixing
  Android OTA/system files with BOOT/display, MCU, CAN, or board configuration files.

Unaffected work should continue with the blocked capability marked honestly.
