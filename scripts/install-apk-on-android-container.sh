#!/usr/bin/env bash
set -euo pipefail

APK_PATH="${1:-/workspace/app-home-test-mobile.apk}"
PACKAGE_NAME="${2:-com.learnautomationapp}"

echo "Waiting for Android boot..."
until adb devices | grep -Eq 'emulator-[0-9]+[[:space:]]+device' && adb shell getprop sys.boot_completed | grep -q 1; do
  sleep 5
done

if adb shell pm list packages | grep -q "package:${PACKAGE_NAME}"; then
  echo "${PACKAGE_NAME} is already installed."
  exit 0
fi

echo "Installing ${APK_PATH}"
adb install -g -r "${APK_PATH}"
