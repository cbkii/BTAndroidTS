#!/usr/bin/env bash
# validate-magisk-package.sh [expected_package_name] [module_dir]
set -euo pipefail

EXPECTED_PKG="${1:-com.cbkii.btandroidts}"
MODULE_DIR="${2:-magisk/BTAndroidTS}"
ALLOWLIST="${MODULE_DIR}/system/etc/permissions/privapp-permissions-com.cbkii.btandroidts.xml"
APK="${MODULE_DIR}/system/priv-app/BTAndroidTS/BTAndroidTS.apk"
PROP_FILE="${MODULE_DIR}/module.prop"
SCRIPT_FILES=(
  "${MODULE_DIR}/post-fs-data.sh"
  "${MODULE_DIR}/service.sh"
  "${MODULE_DIR}/customize.sh"
)
TEXT_FILES=(
  "${PROP_FILE}"
  "${ALLOWLIST}"
  "${SCRIPT_FILES[@]}"
)

printf 'Validating Magisk module at %s\n' "${MODULE_DIR}"

required_files=(
  "${PROP_FILE}"
  "${ALLOWLIST}"
  "${APK}"
  "${SCRIPT_FILES[@]}"
)
for f in "${required_files[@]}"; do
  if [[ ! -f "${f}" ]]; then
    printf '::error::Missing file: %s\n' "${f}" >&2
    return 1
  fi
done

CR="$(printf '\r')"
for f in "${TEXT_FILES[@]}"; do
  if grep -Iq "${CR}" "${f}"; then
    printf '::error::CRLF line endings detected in module file: %s\n' "${f}" >&2
    return 1
  fi
done

if ! grep -q '^id=btandroidts$' "${PROP_FILE}"; then
  printf '::error::module.prop: id must be '\''btandroidts'\''\n' >&2
  return 1
fi
if ! grep -q '^name=' "${PROP_FILE}"; then
  printf '::error::module.prop: missing name\n' >&2
  return 1
fi
if ! grep -q '^version=' "${PROP_FILE}"; then
  printf '::error::module.prop: missing version\n' >&2
  return 1
fi
if ! grep -q '^versionCode=[0-9]\+$' "${PROP_FILE}"; then
  printf '::error::module.prop: invalid or missing versionCode\n' >&2
  return 1
fi

if ! grep -q "package=\"${EXPECTED_PKG}\"" "${ALLOWLIST}"; then
  printf '::error::Allowlist package mismatch. Expected: %s\n' "${EXPECTED_PKG}" >&2
  grep 'package=' "${ALLOWLIST}" || true
  return 1
fi

if ! grep -q 'android.permission.BLUETOOTH_PRIVILEGED' "${ALLOWLIST}"; then
  printf '::error::Allowlist missing BLUETOOTH_PRIVILEGED\n' >&2
  return 1
fi

if grep -Eq 'android.permission.BLUETOOTH_STACK|android.uid.system|sharedUserId' "${ALLOWLIST}"; then
  printf '::error::Unsafe privilege found in privapp allowlist\n' >&2
  return 1
fi

PERM_COUNT="$(awk '{ count += gsub(/<permission[[:space:]]/, "&") } END { print count + 0 }' "${ALLOWLIST}")"
if [[ "${PERM_COUNT}" -ne 1 ]]; then
  printf '::error::Privapp allowlist grants an unexpected number of permissions (%s). Expected: 1\n' "${PERM_COUNT}" >&2
  return 1
fi
PERMISSION_NAME="$(awk -F'"' '/<permission[[:space:]]/{print $2; return}' "${ALLOWLIST}")"
if [[ "${PERMISSION_NAME}" != "android.permission.BLUETOOTH_PRIVILEGED" ]]; then
  printf '::error::Unexpected privapp permission: %s\n' "${PERMISSION_NAME}" >&2
  return 1
fi

extra_text_files=(
  "${MODULE_DIR}/META-INF/com/google/android/update-binary"
  "${MODULE_DIR}/META-INF/com/google/android/updater-script"
)
DANGEROUS_DIRECTIVE_RE='android\.permission\.BLUETOOTH_STACK|android\.uid\.system|sharedUserId|(^|[[:space:]])setenforce([[:space:]]|$)|(^|[[:space:]])mount[[:space:]].*-o[[:space:]]*([^[:space:]]*,)*rw(,|[[:space:]]|$)|pm[[:space:]]+clear[[:space:]]+com\.android\.bluetooth'

for f in "${TEXT_FILES[@]}" "${extra_text_files[@]}"; do
  if [[ -f "${f}" ]] && grep -Eq "${DANGEROUS_DIRECTIVE_RE}" "${f}"; then
    printf '::error::Unsafe privileged-module directive found in %s\n' "${f}" >&2
    return 1
  fi
done

printf 'BTAndroidTS Magisk module validation passed for package: %s\n' "${EXPECTED_PKG}"
