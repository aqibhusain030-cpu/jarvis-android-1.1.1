#!/usr/bin/env bash
set -euo pipefail
python3 - <<'PY'
import json, pathlib
root = pathlib.Path('.')
a = json.loads((root/'app/src/main/assets/config.json').read_text(encoding='utf-8'))
assert a['safety']['pin_required_for_high_risk'] is True
assert a['safety']['cloud_fallback_enabled'] is False
assert a['safety']['kill_phrase_enabled'] is True
assert a['autonomy']['max_active_goals'] >= 1
assert a['autonomy']['max_tool_calls_per_goal'] >= 1
assert (root/'third_party/LOCKFILE').exists()
assert (root/'gradlew').exists() and (root/'gradlew.bat').exists()
assert 'distributionSha256Sum=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6' in (root/'gradle/wrapper/gradle-wrapper.properties').read_text()
manifest=(root/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
for forbidden in ('MANAGE_EXTERNAL_STORAGE','QUERY_ALL_PACKAGES','ACCESS_BACKGROUND_LOCATION','READ_SMS','RECEIVE_SMS','READ_CALL_LOG','WRITE_CALL_LOG','BLUETOOTH_SCAN','NEARBY_WIFI_DEVICES','CAMERA'):
    assert forbidden not in manifest, forbidden
assert 'android:usesCleartextTraffic="false"' in manifest
if 'BIND_ACCESSIBILITY_SERVICE' in manifest:
    assert 'android:name=".capability.accessibility.JarvisAccessibilityService"' in manifest
    assert 'android:exported="false"' in manifest
    assert manifest.count('BIND_ACCESSIBILITY_SERVICE') == 1
assert 'android:foregroundServiceType="microphone"' in manifest
gradle=(root/'app/build.gradle.kts').read_text(encoding='utf-8')
assert 'compileSdk = 36' in gradle
assert 'versionCode = 3' in gradle
assert 'versionName = "1.1.1"' in gradle
assert 'targetSdk = 36' in gradle
assert 'abortOnError = true' in gradle
assert 'checkReleaseBuilds = true' in gradle
assert not (root/'app/src/main/kotlin/com/jarvis/security/SecurityUnlockActivity.kt').exists()
assert not (root/'app/src/main/kotlin/com/jarvis/service/BootReceiver.kt').exists()
print('release metadata/security checks passed')
PY
