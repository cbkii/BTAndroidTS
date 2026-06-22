# Bluetooth Lane Model

Audience: developers, reviewers, maintainers, and AI agents. This file explains the separation
between Android peripheral Bluetooth and the protected Topway automotive lane.

BTAndroidTS uses a tandem model. It improves user-facing management of Android peripherals without
replacing the Bluetooth stacks.

## Evidence Basis

- Observed: `com.android.bluetooth` is the Android framework package at `/system/app/Bluetooth`,
  shared user `android.uid.bluetooth` / UID 1002. The captured adapter was `Toparea`
  (`A8:82:C3:E3:E7:5A`) in state `BLE_ON`, with zero stack crashes and a bonded `SmartRemote`
  (`DE:8F:7D:8E:A3:1E`).
- Observed: package/service resolver evidence declares GATT, OPP, HID Host, HID Device, PAN, A2DP,
  A2DP Sink, Headset, Headset Client, AVRCP, PBAP, MAP, and related Bluetooth services. Declaration
  is not proof of successful profile connection.
- Observed: `com.tw.bt` is a privileged persistent UID 1000 system package. It has boot/ACC
  receivers and phone/Bluetooth permissions, and must be treated as a protected vendor owner.
- Observed: vendor properties identify `com.zjinnova.zlink` as the phone-connect app and expose a
  Topway/ZLink Bluetooth address `00:87:61:A9:26:26`. ZLink is a normal data app, not a UID 1000
  privileged package.
- Observed: the OEM PDF states TS18 Bluetooth 5.0 support for phonebook, A2DP, Bluetooth OBD,
  mouse, gamepad, keyboard, and other external Bluetooth devices. This supports hardware
  suitability but still requires runtime validation on this firmware.

## Android/Unisoc Peripheral Lane

Owner: `com.android.bluetooth`

Intended use:

- Classic discovery.
- BLE discovery and GATT inspection.
- Bonding and selective unpairing.
- HID Host for keyboards, mice, game controllers, remotes, and phone HID apps.
- Android input verification.
- PAN where available.
- OPP/file transfer delegation where available.

BTAndroidTS may supervise this lane through Android APIs, privileged APIs where granted, and narrow
root fallbacks where proven safe. It must use finite timeouts and observable state changes.
It must not edit framework pairing databases, replace stack components, or assume a declared profile
service will accept a connection.

## Topway Automotive Lane

Owner: `com.tw.bt`, `com.tw.service*`, `blink`/`gocsdk`, and related Topway services.

Protected use:

- HFP calls.
- Phone A2DP and AVRCP.
- PBAP contacts.
- Launcher integration.
- TLink/ZLink and Android Auto integration.

BTAndroidTS must treat this lane as read-only unless an exact-device, capability-gated integration
is proven. The manager should launch the vendor UI when users need phone Bluetooth settings.
It must not create competing media sessions, take audio focus for phone/profile ownership, or disable
vendor projection/phone services to make Android Bluetooth behavior cleaner.

## Protected Packages

BTAndroidTS must not disable, replace, clear data for, or interfere with:

- `com.tw.bt`
- `com.tw.service`
- `com.tw.service.*`
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
- `com.zjinnova.zlink`
- `com.google.android.projection.gearhead`
- `com.dofun.variety`
- `com.dofun.carsetting`
- `com.abupdate.fota_demo_iot`
- `com.sprd.systemupdate`
- TLink/ZLink packages
- `gocsdk`
- `blink` native Topway services
- `android.hardware.bluetooth@1.0-service.unisoc`

## Profile Ownership Defaults

- Android HID Host: allowed for saved peripherals after bonding and capability checks. Service
  declaration is observed; connection, policy, and input-node creation remain device validation
  items.
- Android OPP/PAN: allowed only through explicit user actions and capability checks. OPP components
  are observed; outbound picker/delegation behavior remains device validation.
- Android HFP/A2DP/AVRCP/PBAP for normal phones: not automatically assigned.
- Vendor phone profiles: protected by default.

## Suitability Conclusion

The first-pass feature direction is suitable for TS18 if it remains a standards-first Android
peripheral manager with a separated privileged flavor. It is not suitable to become a replacement
Topway phone stack, a Bluetooth stack patcher, a media-session owner, or a firmware recovery tool.

## Evidence Terms

- Observed: direct evidence in supplied captures or repository code.
- Inferred: reasoned conclusion from observed evidence.
- Hypothesis: plausible explanation awaiting validation.
- Requires device validation: code can exist, but no pass claim is allowed without exact-device
  capture.
- Unsupported: action outside the safety model.
