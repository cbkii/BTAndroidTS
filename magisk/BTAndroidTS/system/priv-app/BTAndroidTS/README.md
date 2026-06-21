# APK placement

Audience: release builders and reviewers.

Place the signed `ts18PrivilegedRelease` APK here as `BTAndroidTS.apk` when building the final
Magisk ZIP.

Do not commit APK binaries to source control. The module is intentionally systemless and must not
replace `Bluetooth.apk`, Bluetooth HAL libraries, WCN firmware, Topway services, pairing databases,
or platform-signed UID 1000 packages.
