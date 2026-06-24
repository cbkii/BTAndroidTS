#!/usr/bin/env bash
set -euo pipefail

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "${sdk_root}" ]]; then
  printf '::error::ANDROID_HOME/ANDROID_SDK_ROOT is not set.\n' >&2
  exit 1
fi

apksigner=""
aapt_bin=""

if [[ -d "${sdk_root}/build-tools" ]]; then
  apksigner="$(find "${sdk_root}/build-tools" -mindepth 2 -maxdepth 2 -type f -name apksigner -perm -111 2>/dev/null | sort -V | tail -n 1 || true)"
  aapt_bin="$(find "${sdk_root}/build-tools" -mindepth 2 -maxdepth 2 -type f -name aapt -perm -111 2>/dev/null | sort -V | tail -n 1 || true)"
fi

if [[ -z "${apksigner}" || -z "${aapt_bin}" ]]; then
  printf '::error::Required Android build tools not found under %s\n' "${sdk_root}" >&2
  exit 1
fi

mapfile -t release_apks < <(
  find app/build/outputs/apk -path '*/release/*.apk' -type f ! -name '*unsigned*' | sort
)

if (( ${#release_apks[@]} == 0 )); then
  printf '::error::No signed-looking release APKs found under app/build/outputs/apk/**/release.\n' >&2
  find app/build/outputs/apk -type f -name '*.apk' -print >&2 || true
  exit 1
fi

missing=0
for expected in standard ts18Privileged; do
  if ! printf '%s\n' "${release_apks[@]}" | grep -q "/${expected}/release/"; then
    printf '::error::Missing release APK for flavor: %s\n' "${expected}" >&2
    missing=1
  fi
done
if [[ "${missing}" -ne 0 ]]; then
  exit 1
fi

for apk in "${release_apks[@]}"; do
  printf 'Validating release APK: %s\n' "${apk}"

  if [[ "${apk}" == *unsigned* ]]; then
    printf '::error::Unsigned APK selected: %s\n' "${apk}" >&2
    exit 1
  fi

  "${apksigner}" verify --verbose --print-certs "${apk}"
  "${aapt_bin}" dump badging "${apk}" | grep -E "^package: name='com\\.cbkii\\.btandroidts" >/dev/null

  if "${aapt_bin}" dump badging "${apk}" | grep -q "application-debuggable"; then
    printf '::error::Release APK is debuggable: %s\n' "${apk}" >&2
    exit 1
  fi

  unzip -t "${apk}" >/dev/null
done

printf 'Release APK validation passed for %d APK(s).\n' "${#release_apks[@]}"

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
