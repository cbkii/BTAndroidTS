#!/usr/bin/env bash
set -euo pipefail

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "${sdk_root}" ]]; then
  printf '::error::ANDROID_HOME/ANDROID_SDK_ROOT is not set.\n' >&2
  exit 1
fi

build_tools_dir="$(find "${sdk_root}/build-tools" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort -V | tail -n 1 || true)"
if [[ -z "${build_tools_dir}" ]]; then
  printf '::error::No Android build-tools directory found under %s\n' "${sdk_root}" >&2
  exit 1
fi

apksigner="${build_tools_dir}/apksigner"
aapt_bin="${build_tools_dir}/aapt"
zipalign="${build_tools_dir}/zipalign"

for tool in "${apksigner}" "${aapt_bin}" "${zipalign}"; do
  if [[ ! -x "${tool}" ]]; then
    printf '::error::Required Android build tool is missing or not executable: %s\n' "${tool}" >&2
    exit 1
  fi
done

declare -A variant_dirs=(
  [standard]="app/build/outputs/apk/standard/release"
  [ts18Privileged]="app/build/outputs/apk/ts18Privileged/release"
)

for variant in standard ts18Privileged; do
  dir="${variant_dirs[$variant]}"
  if [[ ! -d "${dir}" ]]; then
    printf '::error::Release output directory missing for %s: %s\n' "${variant}" "${dir}" >&2
    exit 1
  fi

  mapfile -t apks < <(find "${dir}" -maxdepth 1 -type f -name '*.apk' ! -name '*unsigned*' | sort)
  if (( ${#apks[@]} != 1 )); then
    printf '::error::Expected exactly one signed %s release APK, found %d.\n' "${variant}" "${#apks[@]}" >&2
    find "${dir}" -maxdepth 1 -type f -name '*.apk' -print >&2 || true
    exit 1
  fi

  apk="${apks[0]}"
  printf 'Validating %s release APK: %s\n' "${variant}" "${apk}"

  "${zipalign}" -c -p 4 "${apk}"
  "${apksigner}" verify --verbose --print-certs "${apk}"
  "${aapt_bin}" dump badging "${apk}" | grep -E "^package: name='com\.cbkii\.btandroidts" >/dev/null

  if "${aapt_bin}" dump badging "${apk}" | grep -q "application-debuggable"; then
    printf '::error::Release APK is debuggable: %s\n' "${apk}" >&2
    exit 1
  fi

  unzip -t "${apk}" >/dev/null
done

printf 'Release APK validation passed.\n'
