#!/usr/bin/env bash
set -euo pipefail

if [ -z "${APK_FILE:-}" ]; then
  echo "::error title=APK setup::APK_FILE is required."
  exit 1
fi

if [ -n "${APK_DOWNLOAD_URL:-}" ]; then
  echo "::add-mask::${APK_DOWNLOAD_URL}"
fi

if [ -n "${EXPECTED_APK_SHA256:-}" ]; then
  echo "::add-mask::${EXPECTED_APK_SHA256}"
fi

mkdir -p build/ci-diagnostics
rm -f "${APK_FILE}"

if [ -n "${APK_DOWNLOAD_URL:-}" ]; then
  case "${APK_DOWNLOAD_URL}" in
    https://*) ;;
    *)
      echo "::error title=APK setup::APK_DOWNLOAD_URL must use https://."
      exit 1
      ;;
  esac

  echo "Downloading APK from APK_DOWNLOAD_URL."
  apk_source="url"
  curl --fail --silent --show-error --location \
    --retry 3 --retry-all-errors --retry-delay 10 \
    --connect-timeout 20 --max-time 600 \
    --output "${APK_FILE}" \
    "${APK_DOWNLOAD_URL}"
else
  echo "APK_DOWNLOAD_URL is not configured. Downloading ${APK_FILE} from release tag ${APK_RELEASE_TAG}."
  apk_source="release:${APK_RELEASE_TAG}"
  gh release download "${APK_RELEASE_TAG}" \
    --repo "${GITHUB_REPOSITORY}" \
    --pattern "${APK_FILE}" \
    --dir .
fi

if [ ! -s "${APK_FILE}" ]; then
  echo "::error title=APK setup::Downloaded APK file is empty or missing."
  exit 1
fi

if [ -z "${EXPECTED_APK_SHA256:-}" ]; then
  echo "::error title=APK setup::APK_SHA256 secret is required to validate the downloaded APK."
  exit 1
fi

actual_sha="$(sha256sum "${APK_FILE}" | awk '{print toupper($1)}')"
expected_sha="$(printf '%s' "${EXPECTED_APK_SHA256}" | tr '[:lower:]' '[:upper:]' | tr -d '[:space:]')"

if [ "${actual_sha}" != "${expected_sha}" ]; then
  echo "::error title=APK checksum::Downloaded APK checksum does not match the configured APK_SHA256 secret."
  exit 1
fi

apk_size_bytes="$(wc -c < "${APK_FILE}" | tr -d '[:space:]')"

{
  echo "APK_FILE=${APK_FILE}"
  echo "APK_SOURCE=${apk_source}"
  echo "APK_SIZE_BYTES=${apk_size_bytes}"
} > build/ci-diagnostics/apk.txt

echo "APK checksum verified."
