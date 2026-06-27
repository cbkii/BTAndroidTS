# Phone-keyboard compatibility references

## Target behaviour

BTAndroidTS should implement a phone-keyboard compatibility workflow that:

1. keeps the Topway phone/projection lane protected;
2. scans BLE first with bounded low-latency passes;
3. preserves ambiguous BLE candidates, including unnamed and non-connectable results;
4. optionally runs Classic discovery as a second stage;
5. ranks candidates by HID evidence rather than friendly phone names;
6. pairs/bonds only the selected candidate;
7. uses the isolated HID Host controller where available;
8. verifies `InputDevice` creation and real key events before success.

## Sender-side Android HID Device references

Inspect:

- `docs/reference/repos/kontroller/src`
- `docs/reference/repos/hidperipheral/src`
- `docs/reference/repos/usbtoblhid/src`

Search terms:

```text
BluetoothHidDevice
BluetoothHidDeviceAppSdpSettings
BluetoothHidDeviceAppQosSettings
BluetoothProfile.HID_DEVICE
getProfileProxy
registerApp
unregisterApp
sendReport
onAppStatusChanged
onConnectionStateChanged
report descriptor
```

What to extract:

- foreground/registration assumptions;
- user-facing setup steps;
- profile availability failure handling;
- SDP/report descriptor mapping;
- host connection and send-report state machine.

Do not extract:

- app-specific UI structure;
- assumptions that the sender phone is the host;
- root/system modifications from sender projects.

## BLE/HOGP references

Inspect:

- `docs/reference/repos/ble-hid-peripheral-for-android/src`
- `docs/reference/repos/ble-hid-example/src`
- `docs/reference/repos/emubthid/src` for Classic HID contrast only.

Search terms:

```text
00001812
HID_SERVICE
Report Map
ReportReference
HID Information
BluetoothGattServer
startAdvertising
AdvertiseData
ScanRecord
```

What to extract:

- HOGP service UUID and report-map evidence;
- naming/advertising patterns;
- when services may not be visible until connection/bonding;
- cross-platform host behaviour.

## Scanning references

Inspect:

- `docs/reference/repos/android-scanner-compat-library/src`
- `docs/reference/repos/opendroneid-receiver-android/src`

Search terms:

```text
ScanSettings
SCAN_MODE_LOW_LATENCY
setReportDelay
setLegacy
setPhy
PHY_LE_ALL_SUPPORTED
isLeCodedPhySupported
isLeExtendedAdvertisingSupported
isConnectable
ScanCallback
onBatchScanResults
match lost
```

What to extract:

- bounded scan pass construction;
- immediate result delivery;
- capability checks before extended/PHY scan modes;
- candidate TTL/match-lost ideas;
- callback threading and error handling.

## Android/AOSP host-side references

Use official/AOSP source links in `REFERENCE_MANIFEST.md` for:

- `BluetoothHidDevice` sender-side lifecycle;
- `BluetoothLeScanner` and `ScanSettings.Builder` scan semantics;
- AOSP `HidHostService` hidden/privileged state and policy boundaries;
- Android Bluetooth architecture and stack/HAL separation.

## Candidate ranking evidence

Suggested evidence fields for `PhoneKeyboardCandidate`:

```text
firstSeenAt
lastSeenAt
seenCount
transport: Classic / BLE / Both
bondState
name/displayName/advertisedName/scanRecordName
address redacted by default
addressType: public / random / unknown
serviceUuids
hasHidService1812
manufacturerDataPresent
serviceDataPresent
isConnectable: true / false / unknown
lastRssi sampled
hidProfileState
inputVerificationState
protectedTopwayRisk
lastFailureReason
```

## Failure taxonomy

Keep these as domain results, not raw strings:

```text
PHONE_APP_NOT_ADVERTISING
BLE_SEEN_NOT_CONNECTABLE
PAIRING_TIMEOUT
PAIRING_REJECTED_BY_PHONE
BOND_CREATED_NO_HID_SERVICE
HID_SERVICE_SEEN_CONNECT_FAILED
HID_CONNECTED_NO_INPUT_DEVICE
INPUT_DEVICE_ACTIVE_NO_KEY_EVENTS
TOPWAY_CONFLICT_RISK
PRIVILEGED_HID_HOST_REQUIRED
UNSUPPORTED_TS18_STACK
```
