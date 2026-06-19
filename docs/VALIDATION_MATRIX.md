# Validation Matrix

No row may be marked passed unless it has exact-device capture evidence. For evidence-only rows,
`Observed passed` means the captured fact exists on the exact device; it is not a runtime pass for a
new BTAndroidTS feature.

| Area | Status | Notes |
| --- | --- | --- |
| Exact TS18 identity captured | Observed passed | `s9863a1h10_Natv`, `uis8581a2h10` / `sp9863a`, `TS18.2.2_20241210.165912_WINDOW-THEME1`, Android 10 / SDK 29. |
| Root state captured | Observed passed | Magisk 28.1, `uid=0`, `u:r:magisk:s0`, SELinux permissive, AVB orange/unlocked, verity enforcing, no installed Magisk modules. |
| Android Bluetooth package present | Observed passed | `com.android.bluetooth` at `/system/app/Bluetooth`, shared user `android.uid.bluetooth` / UID 1002, target SDK 29. |
| Android Bluetooth baseline | Observed passed | Adapter `Toparea` / `A8:82:C3:E3:E7:5A`, state `BLE_ON`, zero crashes, no active GATT clients/servers in capture. |
| Profile services declared | Observed passed | GATT, OPP, HID Host, HID Device, PAN, A2DP, Headset, AVRCP, PBAP, MAP declarations were present. This is not a connection pass. |
| `SmartRemote` bond baseline | Observed passed | `DE:8F:7D:8E:A3:1E` appeared as a bonded LE device. Input-node creation was not proven. |
| Topway lane baseline | Observed passed | `com.tw.bt` is privileged persistent UID 1000; ZLink/Topway Bluetooth properties and services were present. |
| OEM TS18 board support reviewed | Observed passed | OEM PDF documents UIS8581A Android 10 TS18 board family, USB Host/OTG separation, and Bluetooth keyboard/mouse/gamepad/OBD support statements. |
| USB/storage baseline | Observed passed | FAT `usbdisk0` and `usbdisk1` mounted with `/storage/usbdiskN` app-facing paths. |
| DocumentsUI absence baseline | Observed passed | `com.android.documentsui` was not found; external storage and media providers were present. |
| Display/input baseline | Observed passed | 1280x720 physical, 1225x720 app area, stable content `[0,55]-[1225,720]`, raw touch 1024x600 scaled. |
| Standard APK builds | Not tested | Code unchanged in this second pass; first-pass local build is repository validation, not exact-device runtime evidence. |
| TS18 privileged APK builds | Not tested | Code unchanged in this second pass; privileged grant still needs on-device validation after systemless placement. |
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
| OPP stock delegation | Requires device validation | OPP components are present in package evidence, but picker/delegation behavior is untested. |
| Incoming OPP preserved | Requires device validation | Must not start a competing inbound server. |
| Topway phone lane unaffected | Requires device validation | Includes HFP, A2DP, PBAP, TLink/ZLink checks. |
| Safe mode disables experimental paths | Not tested | Implementation pending. |
| Diagnostics export | Not tested | Implementation pending. |
| USB export to `/storage/usbdiskN` | Requires device validation | Must handle absent DocumentsUI and media removal. |
| UI stable within TS18 insets | Requires device validation | Stable content `[0,55]-[1225,720]`; avoid phone portrait assumptions. |
| Low-impact notifications/jobs | Requires device validation | Capture shows active OEM foreground services and job limits; BTAndroidTS must prove bounded behavior. |
| Cold boot reconciliation | Requires device validation | Must validate on actual TS18. |
| ACC sleep/wake reconciliation | Requires device validation | Must validate from observable Android events. |
| Privileged module disable rollback | Requires device validation | No Magisk module was installed in latest capture. |

## Device Validation States

- Observed passed
- Observed failed
- Requires device validation
- Not tested
- Blocked
