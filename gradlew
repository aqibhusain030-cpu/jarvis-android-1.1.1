#!/usr/bin/env bash
set -euo pipefail

ROOT="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROPS="$ROOT/gradle/wrapper/gradle-wrapper.properties"

read_prop() {
  local key="$1" value
  value="$(sed -n "s#^${key}=##p" "$PROPS" | head -n 1)"
  value="${value//\\:/ :}"
  value="${value//\\:/}"
  value="${value//\\\\:/:}"
  printf '%s' "$value"
}

DIST_URL="$(sed -n 's#^distributionUrl=##p' "$PROPS" | head -n 1 | sed 's#\\:#:#g')"
DIST_SHA="$(sed -n 's#^distributionSha256Sum=##p' "$PROPS" | head -n 1 | tr '[:upper:]' '[:lower:]')"

[[ "$DIST_URL" == https://services.gradle.org/distributions/gradle-*.zip ]] || { echo "Invalid distributionUrl" >&2; exit 2; }
[[ "$DIST_SHA" =~ ^[0-9a-f]{64}$ ]] || { echo "Invalid distributionSha256Sum" >&2; exit 2; }

VERSION="${DIST_URL##*/gradle-}"
VERSION="${VERSION%.zip}"
ARCHIVE_NAME="gradle-${VERSION}"
NAME="gradle-${VERSION%-bin}"
GRADLE_HOME_DEFAULT="${GRADLE_USER_HOME:-${HOME}/.gradle}"
CACHE="$GRADLE_HOME_DEFAULT/jarvis-wrapper/dists/$ARCHIVE_NAME/$DIST_SHA"
ZIP="$CACHE/$ARCHIVE_NAME.zip"
TMP="$CACHE/.download.tmp"
DIR="$CACHE/$NAME"

mkdir -p "$CACHE"

if [[ ! -x "$DIR/bin/gradle" ]]; then
  if [[ ! -f "$ZIP" ]]; then
    command -v curl >/dev/null 2>&1 || { echo "curl is required" >&2; exit 3; }
    rm -f "$TMP"
    curl --fail --location --retry 3 --retry-delay 2 \
      --proto '=https' --tlsv1.2 --silent --show-error \
      -o "$TMP" "$DIST_URL"
    printf '%s  %s\n' "$DIST_SHA" "$TMP" | sha256sum -c -
    mv -f "$TMP" "$ZIP"
  else
    printf '%s  %s\n' "$DIST_SHA" "$ZIP" | sha256sum -c -
  fi

  rm -rf "$DIR"
  mkdir -p "$CACHE/.extract"
  rm -rf "$CACHE/.extract"/*
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$ZIP" -d "$CACHE/.extract"
  else
    command -v jar >/dev/null 2>&1 || { echo "unzip or Java jar is required" >&2; exit 4; }
    (cd "$CACHE/.extract" && jar xf "$ZIP")
  fi
  [[ -x "$CACHE/.extract/$NAME/bin/gradle" ]] || { echo "Gradle distribution layout invalid" >&2; exit 5; }
  mv "$CACHE/.extract/$NAME" "$DIR"
  rm -rf "$CACHE/.extract"
fi

exec "$DIR/bin/gradle" "$@"
