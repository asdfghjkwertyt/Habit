#!/usr/bin/env bash
set -euo pipefail

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Install Android Platform Tools and ensure adb is in PATH."
  exit 1
fi

if ! adb get-state >/dev/null 2>&1; then
  echo "No connected Android device/emulator. Start one, then retry."
  exit 1
fi

connected_count="$(adb devices | awk 'NR>1 && $2=="device" {count++} END {print count+0}')"
if [[ "$connected_count" -lt 1 ]]; then
  echo "No authorized Android device/emulator found."
  echo "Tip: accept USB debugging prompt or start an emulator from AVD Manager."
  exit 1
fi

echo "ADB device check passed: $connected_count device(s) connected."
