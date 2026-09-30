#!/usr/bin/env bash
set -euo pipefail
python3 - <<'PY'
from pathlib import Path
root=Path('.')
manifest=(root/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
gradle=(root/'app/build.gradle.kts').read_text(encoding='utf-8')
assert 'targetSdk = 36' in gradle
assert 'compileSdk = 36' in gradle
assert 'FOREGROUND_SERVICE_DATA_SYNC' not in manifest
if 'BIND_ACCESSIBILITY_SERVICE' in manifest:
    assert 'android:name=".capability.accessibility.JarvisAccessibilityService"' in manifest
    assert 'android:exported="false"' in manifest
    assert manifest.count('BIND_ACCESSIBILITY_SERVICE') == 1
assert 'MANAGE_EXTERNAL_STORAGE' not in manifest
assert 'QUERY_ALL_PACKAGES' not in manifest
assert 'ACCESS_BACKGROUND_LOCATION' not in manifest
assert 'android:usesCleartextTraffic="false"' in manifest
assert 'android:allowBackup="false"' in manifest
assert 'android:foregroundServiceType="microphone"' in manifest
assert not (root/'app/src/main/kotlin/com/jarvis/service/BootReceiver.kt').exists()
assert not (root/'app/src/main/kotlin/com/jarvis/security/SecurityUnlockActivity.kt').exists()
print('release manifest gate passed')
PY
