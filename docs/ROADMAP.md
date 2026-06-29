# BTAndroidTS Roadmap

Keep this file short. Move completed implementation detail into code or release notes, and keep only active/incomplete work here.

## Merge / Release Readiness Checklist

Before merging a feature PR:

- Unit tests pass.
- Android Build CI passes.
- Lint passes or any findings are explicitly justified.
- No app/runtime behavior is claimed as exact-TS18 validated unless captured on the device.
- Review comments are either fixed, stale, or explicitly skipped with a reason.
- No protected Topway/vendor service behavior is changed without an exact-device rollback plan.

## TS18 UI Validation Still Needed

Manual validation on the exact TS18 remains required for:

- 1280x720 visual inspection.
- App area around 1225x720 and stable content around 1225x665.
- Status/nav inset behavior.
- Expanded/collapsed sidebar width and touch usability.
- Cold start and process restart.
- ACC sleep/wake behavior.

## Runtime Validation Still Needed

Do not mark these as passed without exact-device evidence:

- HID Host keyboard/remote/controller/phone-as-keyboard connection.
- Android input-node creation and actual typing.
- OPP send/receive flows, destination selection, progress, cancellation, and failure modes.
- Magisk privileged install, permission grant, disable/remove rollback.
- Coexistence with Topway Bluetooth, calls, phone audio, contacts, DoFun, ZLink/TLink/Android Auto.

## Explicitly Unsupported

- Replacing Android Bluetooth stack or vendor Topway services.
- Direct firmware/HAL/MCU/CAN/BOOT/display partition writes.
- Broad SELinux or root-service changes.
- Large third-party source snapshots, generated review exports, prompt transcripts, CI logs, or zipped research packs in the repo.
