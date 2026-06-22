# BTAndroidTS Magisk Module

Audience: release builders, reviewers, maintainers, and AI agents. End users should receive a
prebuilt module with rollback instructions.

This module is the privileged TS18 deployment shape for `com.cbkii.btandroidts`.

It must contain:

- `system/priv-app/BTAndroidTS/BTAndroidTS.apk` from the `ts18PrivilegedRelease` build.
- `system/etc/permissions/privapp-permissions-com.cbkii.btandroidts.xml`.
- `module.prop`.
- bounded `post-fs-data.sh` and `service.sh` scripts.

Rollback:

1. Create `disable` in the module directory or disable the module in Magisk Manager.
2. Reboot.
3. Verify `com.android.bluetooth`, `com.tw.bt`, ZLink, media, calls, and projection still behave as
   stock.
4. Remove BTAndroidTS app data only after the module is disabled if a clean reset is required.

STOP if validation requires replacing Bluetooth stack components, writing partitions, disabling
Topway services, or changing SELinux policy.
