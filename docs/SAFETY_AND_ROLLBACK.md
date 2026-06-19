# Safety and Rollback

## Exact-Device Constraints

- Observed: root is Magisk 28.1 with `uid=0` and context `u:r:magisk:s0`, but this does not provide
  platform signing, UID 1000, signature permissions, or vendor identity.
- Observed: `/` and `/vendor` are read-only device-mapper mounts and were already space-constrained
  in the capture. BTAndroidTS work must avoid direct writes there and avoid large local extraction or
  logging.
- Observed: no Magisk modules were installed during the capture. Any privileged deployment is a new
  risk surface and needs a disable path before first boot-time enablement.
- Observed: `com.tw.bt`, ZLink, DoFun, SystemUI, MediaProvider, external storage, updaters, and
  vendor Bluetooth services are active/protected. Their data and enabled state are rollback
  boundaries, not cleanup targets.
- Observed: DocumentsUI is absent while external storage/media providers exist. Recovery/export
  flows must have a direct `/storage/usbdiskN` fallback and must handle media removal.
- Observed: the OEM PDF documents USB Host vs USB OTG separation and U-disk/SD recovery precedents.
  Those pages are hardware/recovery evidence, not approval to flash or change partitions from this
  app.

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
- use raw `/mnt/media_rw` paths from normal app code when `/storage/usbdiskN` is available.
- start recurring jobs, foreground services, or notifications without bounded work and user-visible
  purpose.
- take over `com.tw.media` media-button ownership or audio focus for ordinary peripheral work.

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
- never issue `pm disable`, `pm clear`, `am force-stop`, `settings put`, `setprop`, `svc bluetooth`,
  or reboot commands against protected packages/services unless a task-specific validation plan and
  approval exist.

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

The first privileged install should be treated as a staged validation:

1. Install the standard APK and confirm no privileged permission is requested.
2. Install/place the privileged flavor systemlessly only after the standard path is stable.
3. Confirm `BLUETOOTH_PRIVILEGED` grant status on-device before enabling privileged actions.
4. Validate Topway phone/projection/media behavior before and after Android Bluetooth actions.
5. Capture boot ID, package state, Bluetooth manager state, logs, and rollback state after the test.

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
- a proposed feature needs UID 1000, platform signing, `BLUETOOTH_STACK`, or direct edits under
  `/data/misc/bluedroid`.
- the device enters repeated notification crashes, job churn, Bluetooth restarts, thermal throttling,
  memory pressure, or ACC wake regressions.

Unaffected work should continue with the blocked capability marked honestly.
