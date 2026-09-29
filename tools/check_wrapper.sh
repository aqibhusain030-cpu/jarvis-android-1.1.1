#!/usr/bin/env bash
set -euo pipefail
[[ -x gradlew ]] || { echo "gradlew must be executable" >&2; exit 1; }
[[ -f gradlew.bat && -f gradle/wrapper/gradle-wrapper.properties ]] || exit 1
grep -q '^distributionUrl=https\\://services.gradle.org/distributions/gradle-8.11.1-bin.zip$' gradle/wrapper/gradle-wrapper.properties
grep -q '^distributionSha256Sum=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6$' gradle/wrapper/gradle-wrapper.properties
! grep -q '__GRADLE_DIST_SHA256__' gradle/wrapper/gradle-wrapper.properties
! grep -q 'gradle-wrapper.jar' .gitignore
echo "secure Gradle bootstrap wrapper gate passed"
