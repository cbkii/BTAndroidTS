# Feature Completeness Audit

| Feature | Current Implementation Status | User-visible behaviour | Validation Status | Exact files touched |
| :--- | :--- | :--- | :--- | :--- |
| Connected device screen BACK behaviour | Implemented correct navigation semantics. | BACK only warns when tearing down active RFCOMM terminal. Dialog copy explicitly says "Close Terminal Session". | Requires device validation | `strings.xml`, `BTClassicClientScreen.kt` |
| Connection Profile / Available Features UX | Human-readable descriptors for UUIDs. | Two-column layout with human-readable name and UUID. "Try all supported methods" FAB added. | Requires device validation | `SelectableUUIDCard.kt`, `BluetoothProfileRoute.kt`, `BTDeviceProfileScreen.kt` |
| File Sharing Workflow | Standalone flow using stock Android OPP delegation. | File Sharing dashboard item navigates to OppHistoryScreen. File picker added to select target and initiate transfer. | Requires device validation | `BTDevicesScreen.kt`, `OppHistoryScreen.kt`, `OppHistoryViewModel.kt` |
| Keyboards / Peripherals Workflow | Specific device listing and routing to PeripheralDetail. | Dashboard navigates to PeripheralManagerScreen to select and manage bonded peripherals. | Requires device validation | `PeripheralManagerScreen.kt`, `BTDevicesScreen.kt`, `ViewModelModule.kt` |
| Keyboard test crash & UI | Handled crashes, added native platform fallback. | Keyboard Test screen uses AndroidView wrapping EditText with setOnKeyListener to gracefully handle missing focus and prevent crashes. | Requires device validation | `KeyboardTestScreen.kt` |
| Text fields do not work | Native AndroidView fallback for SendCommandTextField. | Replaced BasicTextField with native AndroidView EditText to resolve Compose focus bugs on TS18 hardware. | Requires device validation | `SendCommandTextField.kt` |
| Plain-English UX and terminology | Replaced technical jargon. | Replaced UUID descriptors and settings menus with plain-English. | Requires device validation | `strings.xml` |

## Validation Notes
TS18 validation required for exact behavior of native EditText and File Picker.
