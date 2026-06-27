# Phone Keyboard Compatibility Implementation Notes

## Phase 1 - Phone keyboard domain model
* Will add `PhoneKeyboardCandidate`, `PhoneKeyboardScanEvidence`, `PhoneKeyboardCompatibilityMode`, `PhoneKeyboardFailureReason`, `PhoneKeyboardUserGuidance`, `PhoneKeyboardValidationState` to `com.cbkii.btandroidts.domain.peripheral` or a new `domain.phone_keyboard` package.

## Phase 2 - Dedicated staged phone-keyboard scan controller
* Create `PhoneKeyboardScanController` implementing staged BLE passes.
* Need to use existing wrappers or add to them to allow low latency, no service filter, immediate results, extended/non-legacy.

## Phase 3 - Candidate preservation, merge, ranking, and protection policy
* Create `PhoneKeyboardCandidateMerger` or similar logic.

## Phase 4 - UI / UX
* Create `PhoneKeyboardScreen` in the Compose UI.

## Phase 5 - Pair / connect / verify flow
* Connect via `HidHostController` and `BluetoothBondController`.
* Connect via `KeyboardInputVerifier` and `AndroidInputDeviceRepository`.

## Phase 6 - Failure taxonomy and guidance
* Map `PhoneKeyboardFailureReason` to user-facing strings and checklist instructions.

## Phase 7 - Data-only sender-app compatibility guide
* Create data structures containing instructions for generic apps and `BluetoothHidDevice` apps.

## Phase 8 - Tests
* Write unit tests for merging, ranking, scan flow, etc.

## Phase 9 - Documentation
* Update README, implementations status, etc.
