# Validation Matrix

No row may be marked passed unless it has exact-device capture evidence.

| Area | Status | Notes |
| --- | --- | --- |
| Standard APK builds | Not tested | Automated validation required. |
| TS18 privileged APK builds | Not tested | Automated validation required. |
| Magisk ZIP structure | Not tested | Package work pending. |
| Classic discovery raw results | Requires device validation | Existing app has Classic scanning, but TS18 manager flow pending. |
| BLE discovery raw results | Requires device validation | Existing app has BLE scanning, but non-connectable filtering must be corrected. |
| Awaited bonding state machine | Not tested | Implementation pending. |
| Protected selective unpairing | Not tested | Implementation pending. |
| HID Host capability detection | Not tested | Implementation pending. |
| HID profile connect/disconnect | Requires device validation | Must be tested on TS18 hardware. |
| Android input-node verification | Requires device validation | Must use `InputManager`/`InputDevice` evidence. |
| Supervisor survives UI closure | Not tested | Implementation pending. |
| Finite auto-reconnect | Not tested | Implementation pending. |
| OPP stock delegation | Requires device validation | Existing `BluetoothOppService` evidence comes from brief. |
| Incoming OPP preserved | Requires device validation | Must not start a competing inbound server. |
| Topway phone lane unaffected | Requires device validation | Includes HFP, A2DP, PBAP, TLink/ZLink checks. |
| Safe mode disables experimental paths | Not tested | Implementation pending. |
| Diagnostics export | Not tested | Implementation pending. |
| USB export to `/storage/usbdiskN` | Requires device validation | Must handle absent DocumentsUI and media removal. |
| UI stable within TS18 insets | Requires device validation | Stable content `[0,55]-[1225,720]`; avoid phone portrait assumptions. |
| Cold boot reconciliation | Requires device validation | Must validate on actual TS18. |
| ACC sleep/wake reconciliation | Requires device validation | Must validate from observable Android events. |
| Privileged module disable rollback | Requires device validation | No Magisk module was installed in latest capture. |

## Device Validation States

- Observed passed
- Observed failed
- Requires device validation
- Not tested
- Blocked
