1. **Domain Model for Phone Keyboard Compatibility**
   - Enhance the domain model created in `app/src/main/java/com/cbkii/btandroidts/domain/phone_keyboard/PhoneKeyboardDomain.kt` to fully represent candidates, scan evidence, failure taxonomy, and guidance based on evidence.

2. **Scanner & Policy Layer**
   - Create a `PhoneKeyboardScanController` interface and implementation in `app/src/main/java/com/cbkii/btandroidts/domain/phone_keyboard/` for staged BLE-first and Classic scanning passes.
   - Implement deterministic candidate handling, caching, TTL (Time-To-Live), and merging rules (e.g. merge BLE and Classic candidates appropriately).
   - Integrate with the existing `BluetoothDeviceInventoryRepository` and `BluetoothLEScanner`.

3. **UI for Phone Keyboard Mode**
   - Create a new UI route `PhoneKeyboardRoute.kt` in `app/src/main/java/com/cbkii/btandroidts/presentation/navigation/screens/phone_keyboard/`.
   - Update `AppNavigation.kt` to add the `PhoneKeyboardDestination` route.
   - The UI should display the candidates list separated from generic tabs, showing staging evidence, failure/recovery hints, and candidate details (name, transport, evidence, bond state).

4. **Pair / Connect / Verify Flow**
   - Create a `PhoneKeyboardViewModel` to orchestrate this flow.
   - For a selected candidate: Pair using `BluetoothBondController`, attempt HID connection using `HidHostController`, verify input device via `InputDeviceRepository`.
   - Update the UI to reflect these states.

5. **Failure Taxonomy and Guidance**
   - Ensure the UI maps typed failure reasons (`PhoneKeyboardFailureReason`) to user-actionable text.

6. **Documentation & Compatibility Guide**
   - Add a data-only sender-app compatibility guide.
   - Update `README.md`, `docs/IMPLEMENTATION_STATUS.md`, and `docs/VALIDATION_MATRIX.md` with progress and instructions for testing.

7. **Host-Side Tests**
   - Add unit tests for candidate merging, candidate retention logic, and scanner policy in `app/src/test/.../domain/phone_keyboard/`.

8. **Pre-commit Steps**
   - Ensure proper testing, verification, review, and reflection are done before final submission.
