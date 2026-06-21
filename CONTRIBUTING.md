# Contributing to BTAndroidTS

BTAndroidTS is a TS18-focused Android Bluetooth peripheral manager. Contributions are welcome, but
changes must preserve the safety boundaries documented in [AGENTS.md](AGENTS.md) and `docs/`.

## Contribution Workflow

1. Open an issue before starting substantial work.
2. Describe the exact TS18 behavior, evidence, or bug being addressed.
3. Wait for agreement on the approach when the change affects Bluetooth, root, Magisk, Topway
   packages, storage, diagnostics, or recovery.
4. Work on a topic branch from the current target branch.
5. Keep pull requests focused and include validation results.

## Development Environment

Use an Android/Linux shell environment for commands and scripts. Keep project documentation scoped
to Android/Linux shell workflows.

Required tooling:

- JDK 17.
- Android SDK matching the project compile SDK.
- Android build tools.
- POSIX shell utilities for `scripts/*.sh`.

Recommended validation:

```bash
./gradlew testStandardDebugUnitTest
./gradlew lintStandardDebug
./gradlew assembleStandardDebug
./gradlew assembleStandardRelease
./gradlew assembleTs18PrivilegedRelease
sh scripts/check-manifest-permissions.sh
sh scripts/package-magisk.sh
sh scripts/validate-magisk-package.sh
```

Do not claim a command passed unless it was run successfully.

## Pull Request Requirements

- Preserve package identity `com.cbkii.btandroidts`.
- Preserve the MIT licence and upstream attribution.
- Keep Android Bluetooth APIs out of ViewModels.
- Use domain interfaces and Android-specific `data/` implementations.
- Keep hidden or reflected APIs behind narrow adapters.
- Add or update tests for policy, lifecycle, Bluetooth behavior, OPP, diagnostics, or validation
  logic when touched.
- Update [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md) and
  [docs/VALIDATION_MATRIX.md](docs/VALIDATION_MATRIX.md) when behavior or evidence changes.

## TS18 Claims

Use the project evidence labels:

- Observed
- Inferred
- Hypothesis
- Requires device validation
- Unsupported

Only mark real-device behavior as passed when exact TS18 evidence exists. Emulator, local build, or
static analysis success is not TS18 runtime proof.
