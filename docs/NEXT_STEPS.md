# Next Steps for BTAndroidTS

This document outlines the validation phase now that the peripheral manager implementation is complete.

## 1. On-Device Validation (TS18)

The primary goal is now exact-device validation. The following CUJs must be executed on a real Topway TS18 head unit:

### A. HID Peripheral Onboarding
1. Pair a Bluetooth Classic keyboard.
2. Verify "HID Host" connects (reflected API check).
3. Open "Keyboard Test" screen.
4. Type characters and verify "Success" is recorded in the peripheral policy.
5. Reboot head unit and verify supervisor reconnects the HID keyboard.

### B. OPP File Transfer
1. Open a local file (e.g., via DocumentsUI fallback or File Manager).
2. Share to BTAndroidTS.
3. Select the paired destination.
4. Verify stock Android Bluetooth handles the progress.
5. Check "Transfer History" in BTAndroidTS for the recorded entry.

### C. Topway Lane Coexistence
1. Connect a phone via Topway Bluetooth (for HFP/A2DP).
2. Verify BTAndroidTS labels the device as "Topway Automotive" and protects it from unpairing.
3. Launch ZLink/Android Auto and verify BTAndroidTS dashboard correctly identifies the active projection package.

### D. Diagnostics & Storage
1. Trigger "Export Diagnostics".
2. Verify file is written to `/sdcard/Android/data/com.cbkii.btandroidts/files/diagnostics/`.
3. Insert a USB drive and verify export attempts to use `/storage/usbdiskN` if configured.

## 2. Hardening Refinements
- **Expert Override:** Test the bypass for protected device unpairing.
- **Backoff Jitter:** Verify Supervisor retry timings under varying adapter states.
- **R8 Rules:** Confirm no reflected HID methods are stripped in the `ts18PrivilegedRelease` build.

## 3. Deployment
- Sign release APKs using the production keystore.
- Distribute the Magisk module via the release workflow artifacts.
