# TS18 Validation Matrix

This matrix tracks real-device evidence for BTAndroidTS on Topway TS18.

| Feature Area | CUJ / Test Case | Status | Evidence / Notes |
| --- | --- | --- | --- |
| **Inventory** | Classic discovery | Requires TS18 validation | |
| | BLE discovery | Requires TS18 validation | |
| | Stale record expiry | Inferred | Time-based logic in repository. |
| **Bonding** | Classic keyboard bond | Requires TS18 validation | |
| | Selective unpair | Hypothesis | Should block unpair for Topway lane. |
| **HID Host** | Connection state | Inferred | Reflection logic uses API 29 HID Host. |
| | Input node creation | Requires TS18 validation | |
| **Phone Keyboard** | Compatibility mode | Hypothesis | Dedicated UI and policy logic added, requires TS18 tests with phone apps. |
| **Input UX** | Device matching | Inferred | Logic verified in JVM unit tests. |
| | Success recording | Inferred | Persistence verified in DataStore tests. |
| **OPP** | Share single file | Requires TS18 validation | Delegated to stock Android Bluetooth. |
| | Share text/URL | Requires TS18 validation | |
| | History persistence | Observed | History screen and DataStore store implemented. |
| **Topway** | Lane identification | Observed | Based on known Topway package names. |
| | Projection status | Observed | Dashboard shows ZLink/TLink status. |
| **Supervision** | ACC wake reconnect | Requires TS18 validation | |
| | Backoff retry | Inferred | AlarmManager and backoff logic implemented. |
| **Diagnostics** | Local export | Requires TS18 validation | Path detection for internal/USB. |
| | Redaction | Observed | MAC address masking verified. |
