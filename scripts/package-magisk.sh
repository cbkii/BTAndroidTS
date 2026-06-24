#!/usr/bin/env bash
# package-magisk.sh <apk_path> [zip_suffix]
if [ -z "${BASH_VERSION:-}" ]; then
  exec bash "$0" "$@"
fi
set -euo pipefail

APK_PATH="${1:-app/build/outputs/apk/ts18Privileged/release/app-ts18Privileged-release.apk}"
ZIP_SUFFIX="${2:-}"
MODULE_ID="BTAndroidTS"
SOURCE_MODULE_DIR="magisk/${MODULE_ID}"
BUILD_DIR="build"
STAGE_ROOT="${BUILD_DIR}/magisk-stage"
STAGE_MODULE_DIR="${STAGE_ROOT}/${MODULE_ID}"
DEST_DIR="${STAGE_MODULE_DIR}/system/priv-app/BTAndroidTS"
DEST_APK="${DEST_DIR}/BTAndroidTS.apk"
ALLOWLIST_REL="system/etc/permissions/privapp-permissions-com.cbkii.btandroidts.xml"
ALLOWLIST="${STAGE_MODULE_DIR}/${ALLOWLIST_REL}"
AAPT_BIN=""

checked_aapt_paths=()

add_checked_path() {
  checked_aapt_paths+=("$1")
}

find_aapt() {
  local candidate=""

  if [[ -n "${ANDROID_HOME:-}" && -n "${ANDROID_BUILD_TOOLS:-}" ]]; then
    candidate="${ANDROID_HOME}/build-tools/${ANDROID_BUILD_TOOLS}/aapt"
    add_checked_path "${candidate}"
    if [[ -x "${candidate}" ]]; then
      AAPT_BIN="${candidate}"
      return 0
    fi
  fi

  if [[ -n "${ANDROID_SDK_ROOT:-}" && -n "${ANDROID_BUILD_TOOLS:-}" ]]; then
    candidate="${ANDROID_SDK_ROOT}/build-tools/${ANDROID_BUILD_TOOLS}/aapt"
    add_checked_path "${candidate}"
    if [[ -x "${candidate}" ]]; then
      AAPT_BIN="${candidate}"
      return 0
    fi
  fi

  if candidate="$(command -v aapt 2>/dev/null)" && [[ -n "${candidate}" ]]; then
    add_checked_path "PATH:${candidate}"
    AAPT_BIN="${candidate}"
    return 0
  fi
  add_checked_path "PATH:aapt"

  local sdk_root=""
  for sdk_root in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" /opt/android-sdk; do
    [[ -n "${sdk_root}" ]] || continue
    add_checked_path "${sdk_root}/build-tools/*/aapt"
    if [[ -d "${sdk_root}/build-tools" ]]; then
      candidate="$(find "${sdk_root}/build-tools" -mindepth 2 -maxdepth 2 -type f -name aapt -perm -111 2>/dev/null | sort -V | tail -n 1 || true)"
      if [[ -n "${candidate}" ]]; then
        AAPT_BIN="${candidate}"
        return 0
      fi
    fi
  done

  return 1
}

extract_package_name() {
  local apk="$1"
  "${AAPT_BIN}" dump badging "${apk}" | grep "package: name=" | cut -d"'" -f2
}

cleanup() {
  rm -rf "${STAGE_ROOT}"
}
# trap cleanup EXIT
trap 'exit 1' HUP INT TERM

printf '%s\n' '--- BTAndroidTS Magisk Packaging ---'
printf 'Source APK: %s\n' "${APK_PATH}"

if [[ ! -f "${APK_PATH}" ]]; then
  printf '::error::Source APK not found: %s\n' "${APK_PATH}" >&2
  exit 1
fi
if [[ ! -d "${SOURCE_MODULE_DIR}" ]]; then
  printf '::error::Source Magisk module not found: %s\n' "${SOURCE_MODULE_DIR}" >&2
  exit 1
fi

if ! find_aapt; then
  printf '::error::aapt not found. Checked paths:\n' >&2
  printf '::error::  %s\n' "${checked_aapt_paths[@]}" >&2
  exit 1
fi
printf 'Using aapt: %s\n' "${AAPT_BIN}"

if [[ -z "${ZIP_SUFFIX}" ]]; then
  ZIP_SUFFIX="release"
fi
ZIP_PATH="${BUILD_DIR}/BTAndroidTS-${ZIP_SUFFIX}-magisk.zip"

rm -rf "${STAGE_ROOT}"
mkdir -p "${STAGE_ROOT}" "${DEST_DIR}" "${BUILD_DIR}"
cp -a "${SOURCE_MODULE_DIR}" "${STAGE_ROOT}/"
rm -f "${DEST_APK}"
mkdir -p "${DEST_DIR}"
cp "${APK_PATH}" "${DEST_APK}"

if [[ ! -f "${ALLOWLIST}" ]]; then
  printf '::error::Privapp allowlist not found in staged module: %s\n' "${ALLOWLIST}" >&2
  exit 1
fi

PKG_NAME="$(extract_package_name "${DEST_APK}")"
if [[ -z "${PKG_NAME}" ]]; then
  printf '::error::Failed to extract package name from APK with aapt: %s\n' "${DEST_APK}" >&2
  exit 1
fi
printf 'Detected package: %s\n' "${PKG_NAME}"

IS_DEBUG=false
if [[ "${PKG_NAME}" == *.debug ]]; then
  IS_DEBUG=true
fi

ORIG_ALLOWLIST_PKG="$(grep "privapp-permissions package=" "${ALLOWLIST}" | sed -e 's/.*package="//' -e 's/".*//' | head -1)"
if [[ -z "${ORIG_ALLOWLIST_PKG}" ]]; then
  printf '::error::Failed to read package name from staged allowlist: %s\n' "${ALLOWLIST}" >&2
  exit 1
fi

if [[ "${PKG_NAME}" != "${ORIG_ALLOWLIST_PKG}" ]]; then
  printf 'Patching staged allowlist package from %s to %s\n' "${ORIG_ALLOWLIST_PKG}" "${PKG_NAME}"
  sed -i "s/package=\"${ORIG_ALLOWLIST_PKG}\"/package=\"${PKG_NAME}\"/g" "${ALLOWLIST}"
fi

bash scripts/validate-magisk-package.sh "${PKG_NAME}" "${STAGE_MODULE_DIR}"

rm -f "${ZIP_PATH}"
(
  cd "${STAGE_MODULE_DIR}"
  zip -q -r -9 "../../BTAndroidTS-${ZIP_SUFFIX}-magisk.zip" .
)

test -f "${ZIP_PATH}"
# trap - EXIT HUP INT TERM

printf 'ZIP Path: %s\n' "${ZIP_PATH}"
printf 'Staged Module: %s\n' "${STAGE_MODULE_DIR}"
printf 'Package: %s\n' "${PKG_NAME}"
printf 'Debug: %s\n' "${IS_DEBUG}"
printf '%s\n' '-------------------------------------'
