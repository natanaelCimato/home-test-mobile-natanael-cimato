#!/usr/bin/env bash
set -euo pipefail

if [ -z "${APK_FILE:-}" ]; then
  echo "::error title=APK setup::APK_FILE is required."
  exit 1
fi

if [ -n "${APK_DOWNLOAD_URL:-}" ]; then
  echo "Downloading APK from APK_DOWNLOAD_URL."
  curl --fail --location --retry 3 --retry-delay 10 \
    --output "${APK_FILE}" \
    "${APK_DOWNLOAD_URL}"
else
  echo "APK_DOWNLOAD_URL is not configured. Downloading ${APK_FILE} from release tag ${APK_RELEASE_TAG}."
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
expected_sha="$(echo "${EXPECTED_APK_SHA256}" | tr '[:lower:]' '[:upper:]')"

if [ "${actual_sha}" != "${expected_sha}" ]; then
  echo "::error title=APK checksum::Downloaded APK checksum does not match the configured APK_SHA256 secret."
  exit 1
fi

echo "APK checksum verified."
