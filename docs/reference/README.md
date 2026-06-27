# BTAndroidTS reference material

This directory exists for agents, engineers, and reviewers working on TS18 Bluetooth peripheral support. It is not runtime code and is not a dependency bundle.

## Scope

The current priority is the Android phone-as-Bluetooth-keyboard workflow:

- the phone runs a sender app and acts as a Bluetooth HID peripheral/device;
- the TS18/BTAndroidTS side acts as HID Host;
- discovery must cover Classic HID and BLE/HOGP candidates;
- pairing alone is not success;
- success requires Android input-device creation and typed input in BTAndroidTS Keyboard Test.

## Evidence labels

Use the repo’s evidence labels in all implementation notes:

- **Observed**: exact TS18 capture, repo code, official docs, inspected APK/firmware, or inspected upstream source.
- **Inferred**: reasoned conclusion from observed evidence.
- **Hypothesis**: plausible but unproven.
- **Requires device validation**: implemented or supported by precedent but not passed on the exact TS18.
- **Unsupported**: outside the approved BTAndroidTS safety model.

## Directory map

```text
docs/reference/
  README.md
  BTTSPLAN_ANALYSIS.md
  PHONE_KEYBOARD_COMPATIBILITY_REFERENCES.md
  REFERENCE_MANIFEST.md
  AGENT_REFERENCE_NAVIGATION.md
  JULES_CODEX_REFERENCE_PROMPT.md
  reference_repos.json
  fetch-reference-repos.sh   # exports true source snapshots; no nested .git dirs
  grep-reference-repos.sh
  local/BTTSplan.md
  repos/<slug>/UPSTREAM.md
  repos/<slug>/AGENT_NOTES.md
  repos/<slug>/src/            # created by fetch-reference-repos.sh
```

## Fetching source snapshots

Run from the BTAndroidTS repo root:

```bash
bash docs/reference/fetch-reference-repos.sh
```

Use `--force` to replace old snapshots. The fetcher clones into `$HOME/tmp` first, then exports tracked files into `repos/<slug>/src`; it does not leave embedded Git repositories inside BTAndroidTS.

## Safety rule

Do not copy code blindly. Use upstream projects to understand API lifecycle, state machines, scan settings, compatibility traps, and test cases. Keep all TS18 integration behind BTAndroidTS adapters and preserve the Topway automotive Bluetooth lane.
