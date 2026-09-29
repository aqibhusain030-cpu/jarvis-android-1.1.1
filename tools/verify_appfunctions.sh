#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

manifest="app/src/main/AndroidManifest.xml"
service="app/src/main/kotlin/com/jarvis/appfunctions/JarvisAppFunctionService.kt"
metadata="app/src/main/res/xml/app_metadata.xml"

grep -q 'android.permission.BIND_APP_FUNCTION_SERVICE' "$manifest"
grep -q 'android.app.appfunctions.schema' "$manifest"
grep -q 'android.app.appfunctions.v2' "$manifest"
grep -q 'android.app.appfunctions.app_metadata' "$manifest"
grep -q 'AppFunctionServiceEntryPoint' "$service"
grep -q 'appFunctionXmlFileName = "jarvis_app_function_service"' "$service"
grep -q '@AppFunction(isDescribedByKDoc = true)' "$service"
grep -q 'getJarvisStatus' "$service"
grep -q 'listGoals' "$service"
grep -q 'getGoal' "$service"
grep -q '<AppFunctionAppMetadata' "$metadata"

echo "AppFunctions source contract passed."

if ! command -v adb >/dev/null 2>&1; then
  echo "ADB unavailable; device smoke test skipped."
  exit 0
fi

if ! adb get-state >/dev/null 2>&1; then
  echo "No connected Android device; device smoke test skipped."
  exit 0
fi

if ! adb shell cmd app_function help >/dev/null 2>&1; then
  echo "Connected device does not expose cmd app_function; device smoke test skipped."
  exit 0
fi

output="$(adb shell cmd app_function list-app-functions)"
grep -q 'com.jarvis' <<<"$output"
grep -q 'JarvisAppFunctionService' <<<"$output"

echo "AppFunctions device registration smoke test passed."
