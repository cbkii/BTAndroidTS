# Bluetooth Lane Model

BTAndroidTS uses a tandem model. It improves user-facing management of Android peripherals without
replacing the Bluetooth stacks.

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

## Protected Packages

BTAndroidTS must not disable, replace, clear data for, or interfere with:

- `com.tw.bt`
- `com.tw.service`
- `com.tw.service.*`
- `com.tw.core`
- `com.tw.coreservice`
- `com.tw.carinfoservice`
- `com.android.bluetooth`
- `com.android.settings`
- `com.zjinnova.zlink`
- TLink/ZLink packages
- `gocsdk`
- `blink` native Topway services

## Profile Ownership Defaults

- Android HID Host: allowed for saved peripherals after bonding and capability checks.
- Android OPP/PAN: allowed only through explicit user actions and capability checks.
- Android HFP/A2DP/AVRCP/PBAP for normal phones: not automatically assigned.
- Vendor phone profiles: protected by default.

## Evidence Terms

- Observed: direct evidence in supplied captures or repository code.
- Inferred: reasoned conclusion from observed evidence.
- Hypothesis: plausible explanation awaiting validation.
- Requires device validation: code can exist, but no pass claim is allowed without exact-device
  capture.
- Unsupported: action outside the safety model.
