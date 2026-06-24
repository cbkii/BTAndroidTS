# TS18 Validation Matrix

This matrix tracks real-device evidence for BTAndroidTS on Topway TS18.

Use only these evidence labels: Observed, Inferred, Hypothesis, Requires device validation, Unsupported.

| Feature Area | CUJ / Test Case | Evidence Label | Evidence / Notes |
| --- | --- | --- | --- |
| **Inventory** | Classic discovery | Requires device validation | Code implements Classic inventory; exact TS18 scan capture is still required. |
| | BLE discovery | Requires device validation | Code implements BLE inventory; exact TS18 scan capture is still required. |
| | Stale record expiry | Inferred | Time-based logic is implemented in repository code; exact TS18 timing capture is still required. |
| **Bonding** | Classic keyboard bond | Requires device validation | Broadcast-waiting bond controller is implemented; TS18 keyboard pairing must be captured. |
| | Selective unpair | Requires device validation | Protected-device checks and hidden `removeBond` call are implemented; privileged TS18 behavior is unproven. |
| **HID Host** | Connection state | Requires device validation | Reflection logic uses API 29 HID Host; proxy availability and state values must be captured on TS18. |
| | Input node creation | Requires device validation | Input-device repository and Keyboard Test UI exist; kernel/input behavior must be captured on TS18. |
| **Input UX** | Device matching | Inferred | Matching logic exists in app code; multi-device TS18 behavior requires physical validation. |
| | Success recording | Inferred | Persistence path is implemented; exact TS18 keyboard verification remains pending. |
| **OPP** | Share single file | Requires device validation | Delegated to stock Android Bluetooth; picker/progress behavior must be captured on TS18. |
| | Share text/URL | Requires device validation | Delegated to stock Android Bluetooth; text/URL behavior must be captured on TS18. |
| | History persistence | Inferred | History screen/store are implemented in code; device transfer history must be validated with a TS18 share. |
| **Topway** | Lane identification | Requires device validation | Known package/name rules are implemented; exact TS18 package and lane evidence must be captured. |
| | Projection status | Requires device validation | Dashboard displays ZLink/TLink package status from package inspection; exact runtime projection state must be captured. |
| **Supervision** | ACC wake reconnect | Requires device validation | Scheduler/retry code exists; ACC sleep/wake reconnect requires exact TS18 validation. |
| | Backoff retry | Inferred | AlarmManager and finite backoff logic are implemented; real timing should be confirmed on TS18. |
| **Diagnostics** | Local export | Requires device validation | Path detection for internal/USB export is implemented; TS18 filesystem behavior must be captured. |
| | Redaction | Inferred | MAC address masking is implemented in code; validate exported TS18 report before sharing. |
