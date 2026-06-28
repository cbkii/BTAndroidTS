# BTAndroidTS

BTAndroidTS is a Bluetooth peripheral manager for Topway TS18 Android head units. It is designed
to help manage keyboards, remotes, controllers, BLE devices, and Bluetooth file sharing on the
Android Bluetooth lane while leaving the factory Topway phone and Android Auto lane alone.

This app is derived from the original MIT-licensed Bluetooth terminal app. The original licence is
preserved in [LICENSE](LICENSE).

## Who This App Is For

Use BTAndroidTS if you have a TS18 / Topway-style Android head unit and want a safer way to inspect
or manage Android Bluetooth peripherals without disturbing factory phone, media, contacts, or
projection features.

Primary target:

- Topway TS18 / UIS8581A / SP9863A units.
- Android 10 / API 29 runtime.
- 1280x720 landscape head-unit screens.
- Standard APK use first; Magisk privileged install only for tested TS18 workflows.

BTAndroidTS is not a replacement for the factory Bluetooth phone app, Android Auto, ZLink/TLink, or
Topway services.

## What It Can Do

- Show a combined Classic Bluetooth and BLE device inventory.
- Keep vendor and protected devices separate from Android peripherals.
- Run bounded Bluetooth scans instead of continuous discovery.
- Preserve the Topway phone/projection lane for calls, phone audio, contacts, and projection.
- Keep RFCOMM terminal and BLE GATT tools under Advanced Tools.
- Accept Android share intents for Bluetooth OPP file sharing and delegate outbound sending to the
  stock Android Bluetooth service.
- Export bounded local diagnostics for troubleshooting.
- Provide a TS18 privileged build shape for Magisk, with a narrow `BLUETOOTH_PRIVILEGED` allowlist.

## What Still Needs Real TS18 Validation

Do not treat these as passed until they have been tested on the actual head unit:

- HID Host keyboard, remote, controller, and phone-as-keyboard connection.
- Android input-node creation and typing.
- Bluetooth OPP destination picking, progress, cancellation, and receive behavior.
- Cold boot, activity close, process death, and ACC sleep/wake reconnection.
- Magisk privileged permission grant and rollback.
- Calls, phone audio, contacts, and ZLink/TLink/Android Auto coexistence.

## Installation

### Standard APK

Install the standard APK first. It is the safest path and does not request privileged Bluetooth
authority.

```bash
adb install -r BTAndroidTS-standard-release.apk
```

Grant the Bluetooth and location permissions Android requests. Android 10/11 can require location
permission for Bluetooth scanning.

### TS18 Magisk Privileged Variant

Use the Magisk variant only when you have a tested rollback path. The module is systemless and is
intended to place the APK under `system/priv-app` with only the matching
`BLUETOOTH_PRIVILEGED` allowlist.

Before installing the module:

- Save current TS18 diagnostics and stock settings.
- Confirm the module can be disabled from Magisk.
- Confirm you can recover from a bad boot.
- Do not replace `Bluetooth.apk`, native Bluetooth libraries, firmware, or Topway apps.

## How To Use

- Open **Phone / Android Auto** to reach the existing Topway phone/projection lane.
- Use **Keyboards / Peripherals** for Android Bluetooth peripherals.
- Use **File Sharing** through Android Share; BTAndroidTS delegates to stock Bluetooth OPP.
- Use **Diagnostics** only when you want a local troubleshooting report.
- Use **Advanced Tools** for RFCOMM terminal and BLE GATT work.

Connection labels are intentionally explicit: RFCOMM terminal, BLE GATT, HID Host, ACL, and OPP are
different actions.

### Classic Bluetooth

- **Devices:** Display a list of paired devices and available unpaired devices.
- **Connect and Interact:** Allow the user to communicate with the connected device
- **Chat Server:** Start a server within the app to connect to other phones and interact with
  messages.
- **Settings:** You can check customize the connection terminal for clients and server.

### Bluetooth Low Energy (BLE)

- **Scan for Devices:** Scan for devices supporting bluetooth low energy
- **Services and Charateristics:** Display available services and characteristics for the connected
  device. Allow users to read ,write or observe values to the characteristics .
- **Server** A BLE Server with battery and enviromental sensing (illuminanace) and various services
- **Settings:** You can customize scan settings for the app to discover your device.

## Screenshots

These are some of the screens shots showing the working of classic bluetooth connection

<p align="center">
   <img src="screenshots/bt_classic_scan.png" width="22%" />
   <img src="screenshots/bt_peer_features.png" width="22%"/>
   <img src="screenshots/bt_client_talking.png" width="22%"/>  
   <img src="screenshots/bt_classic_settings.png" width="22%"/>
</p>

This screenshots shows the working of a bluetooth low energy device connection

<p align="center">
   <img src="screenshots/ble_devices_scanning.png" width="22%" />
   <img src="screenshots/ble_device_profile.png" width="22%"/>
   <img src="screenshots/ble_notify_running.png" width="22%"/>  
   <img src="screenshots/ble_settings.png" width="22%">
</p>

## Safety Rules

BTAndroidTS must not:

- clear all Bluetooth pairings;
- replace the Android Bluetooth stack;
- edit `/data/misc/bluedroid`;
- disable `com.tw.bt`, `com.tw.service*`, ZLink/TLink, DoFun, SystemUI, MediaProvider, or updater
  packages;
- request `BLUETOOTH_STACK`, UID 1000, platform signing, or a system shared UID;
- repeatedly restart Bluetooth;
- write to firmware, HAL, MCU, CAN, BOOT, display, or vendor partitions.

## More Information

Developer, reviewer, and AI-agent guidance lives outside this end-user README:

- [AGENTS.md](AGENTS.md)
- [docs/TS18_DEVICE_CONTEXT.md](docs/TS18_DEVICE_CONTEXT.md)
- [docs/BLUETOOTH_LANE_MODEL.md](docs/BLUETOOTH_LANE_MODEL.md)
- [docs/VALIDATION_MATRIX.md](docs/VALIDATION_MATRIX.md)
- [docs/INSTALLATION_OPERATION_ROLLBACK.md](docs/INSTALLATION_OPERATION_ROLLBACK.md)

## TS18 Dashboard Layout

The main screen is optimized for a 1280x720 landscape display:
- **Large Controls:** Dashboard actions are presented as large cards for easy touch access.
- **TS18 Safe Zones:** Content is padded to avoid the approximate 55px top status bar and right navigation bar.
- **Glanceable States:** Connection states are explicitly labeled (e.g., "Pair", "Bonded", "HID active").
- **Keyboard Test:** A dedicated tool to verify Bluetooth keyboard input is available from the dashboard.
- **Phone Keyboard Mode:** Dedicated workflow to discover and test Android phones acting as Bluetooth keyboards.
- **Advanced Tools:** Low-level tools like RFCOMM terminal and BLE GATT are moved under the "Advanced Tools" section.

### :revolving_hearts: Special Thanks

Special thanks to [upstream/original Author](https://github.com/tuuhin/BTAndroidApp/issues/new)    ヾ(＠⌒ー⌒＠)ノ

If you encounter any problems or bugs in the app, please raise an [issue](https://github.com/cbkii/BTAndroidTS/issues/new)
