# BLE GATT operation queue

## Status and evidence

- **Observed:** BTAndroidTS uses Android `BluetoothGatt` directly for its BLE client lane.
- **Observed:** Android GATT requests are asynchronous; native start acceptance and callback completion are separate outcomes.
- **Implemented:** each `AndroidBLEClientConnector` now owns a bounded operation queue for its active `BluetoothGatt` session.
- **Requires device validation:** exact TS18 behaviour under rapid operations, disconnects, process death and ACC sleep/wake.

## Invariants

1. A connector permits one callback-driven GATT operation in flight at a time.
2. The queue remains held until the expected callback, native start rejection, callback failure, timeout, disconnect, session replacement or closure.
3. Independent connector instances use independent queues and do not share a process-wide mutex.
4. Callback matching includes the active GATT object and, for attribute operations, service UUID, service instance ID, characteristic UUID, characteristic instance ID and descriptor UUID where applicable.
5. A bounded identity gate rejects callbacks from recently retired GATT objects, including callbacks arriving after a replacement connection has started.
6. Disconnect fails active and pending work before invoking the native disconnect operation, then retires and closes the disconnected GATT object.
7. Reconnect creates a fresh GATT object and queue so callbacks from the disconnected session cannot satisfy new operations.
8. Queue capacity and operation waits are bounded.

## Notification and indication transaction

Notification or indication setup is treated as a serialized composite transaction:

1. validate properties and CCCD presence;
2. apply local `setCharacteristicNotification` state;
3. enqueue the CCCD descriptor write;
4. await `onDescriptorWrite`;
5. expose the active state only after callback success;
6. roll back the local state when the descriptor write fails or times out.

Concurrent notification changes are protected by a connector-local mutex. The callback does not issue echo reads or other GATT requests directly. Follow-up work is submitted through the connector so it is serialized by the same session queue.

## Timeout and cancellation policy

The default callback wait is 10 seconds. Android callbacks do not contain an application request identifier, so a callback arriving after a timeout cannot be safely distinguished from the callback for a later identical request. A timeout therefore:

1. fails the active and pending operations;
2. closes the operation queue;
3. marks the connection failed;
4. retires and closes the GATT object;
5. requires a fresh connection before more GATT work.

Cancelling one caller does not release an already-started Android operation early. The queue still waits for its callback or timeout, preventing the following request from overlapping it.

Connection and disconnect waits use separate bounded timeouts. A failed connection or disconnect also closes its GATT object. Closing the connector permanently closes its queue and rejects later requests.

## Validation

Repository validation:

```bash
./gradlew testStandardDebugUnitTest
./gradlew lintStandardDebug
./gradlew assembleStandardDebug
./gradlew assembleStandardRelease
./gradlew assembleTs18PrivilegedRelease
sh scripts/check-manifest-permissions.sh
sh scripts/package-magisk.sh
sh scripts/validate-magisk-package.sh
```

Exact-device validation must exercise rapid RSSI, discovery, MTU, characteristic and descriptor operations; missing callbacks; peripheral power loss; disconnect/reconnect; service changes; notification setup/teardown; process death; launcher restart; reboot; and real ACC sleep/wake. Do not mark TS18 runtime behaviour passed without a capture from the exact device baseline.
