# BTAndroidTS

BTAndroidTS is a TS18-focused Android Bluetooth peripheral manager derived from the original MIT
licensed Bluetooth terminal application. The project keeps the useful Classic RFCOMM terminal and
BLE GATT inspection tools, but the product direction is now a persistent, safe peripheral manager
for rooted Topway TS18 head units.

The app is intended to manage the Android/Unisoc Bluetooth lane while leaving the separate Topway
automotive Bluetooth lane in place for phone calls, media, contacts, and projection.

## Target Runtime

- Android 10 runtime, API 29 minimum.
- Primary exact-device target: UIS8581A / SP9863A TS18 family, model `s9863a1h10_Natv`.
- Rooted deployments should use reversible Magisk modules, not direct system or vendor partition
  modification.
- `com.android.bluetooth` remains the Android Bluetooth stack authority.
- `com.tw.bt` and Topway services remain the automotive phone/projection authority.

See [docs/TS18_DEVICE_CONTEXT.md](docs/TS18_DEVICE_CONTEXT.md) and
[docs/BLUETOOTH_LANE_MODEL.md](docs/BLUETOOTH_LANE_MODEL.md) for evidence classification and
the tandem-lane model.

## Build Variants

The project exposes two distribution flavours:

- `standard`: normal APK install, no privileged Bluetooth permission.
- `ts18Privileged`: privileged APK variant intended for systemless Magisk placement. This flavour
  may request `android.permission.BLUETOOTH_PRIVILEGED` and must remain narrowly scoped.

Both release variants keep package identity `com.cbkii.btandroidts`; debug builds add the existing
debug suffix. This allows the privileged build to use a matching Android priv-app allowlist without
changing the application data identity.

## Current Capabilities

- Classic Bluetooth discovery and RFCOMM terminal tooling inherited from the upstream app.
- BLE discovery and GATT inspection inherited from the upstream app.
- BLE server tools retained as advanced diagnostics.
- TS18 documentation, safety boundaries, validation matrix, and build-flavour baseline added for
  the peripheral-manager transformation.

The project is in active transformation. Device-level HID Host, OPP, supervisor, diagnostics, and
Magisk packaging work is tracked in the validation matrix and must not be reported as TS18-passed
until captured on real hardware.

## Safety Boundaries

BTAndroidTS must not:

- replace `Bluetooth.apk`;
- replace Bluetooth HAL libraries or WCN/controller firmware;
- edit `/data/misc/bluedroid` directly;
- clear all bonds;
- disable Topway services;
- claim UID 1000, platform signing, or `android.permission.BLUETOOTH_STACK`;
- start unbounded scans, root commands, or Bluetooth restarts.

See [docs/SAFETY_AND_ROLLBACK.md](docs/SAFETY_AND_ROLLBACK.md) for rollback and STOP conditions.

## Building

```bash
./gradlew testStandardDebugUnitTest
./gradlew lintStandardDebug
./gradlew assembleStandardDebug
./gradlew assembleStandardRelease
./gradlew assembleTs18PrivilegedDebug
./gradlew assembleTs18PrivilegedRelease
```

Release builds run R8. Reflection-dependent Bluetooth code must use narrow keep rules only.

## Upstream Attribution

This repository is derived from the original Bluetooth terminal application by Tuuhin. The original
MIT licence is preserved in [LICENSE](LICENSE). Useful upstream debugging features are retained
under Advanced Tools rather than removed.
