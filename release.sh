#!/usr/bin/env bash
# Cuts a release: SNAPSHOT -> release version -> full gated deploy (via
# deploy.sh: build, Sonar quality gate, Dependency-Track vulnerability gate,
# Nexus deploy) -> bump pom.xml to the next SNAPSHOT for continued work.
#
# This project has no git repository, so there is no tag/commit step here —
# "release" means: a real, gated artifact reaches Nexus under a non-SNAPSHOT
# version, and pom.xml is left ready for the next development cycle.
#
# Usage:
#   ./release.sh                 # strips "-SNAPSHOT" from the current
#                                 # version to get the release version,
#                                 # then bumps the patch number + "-SNAPSHOT"
#                                 # for the next development version
#   ./release.sh 2.0.0           # release exactly this version instead
#   ./release.sh 2.0.0 2.1.0     # release 2.0.0, continue development as
#                                 # 2.1.0-SNAPSHOT
#
# Everything deploy.sh needs (SONAR_TOKEN, DTRACK_API_KEY, etc.) is still
# required — see deploy.sh's own header for those.

set -uo pipefail
cd "$(dirname "$0")"

log() { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
fail() { printf '\033[1;31mFAILED: %s\033[0m\n' "$1" >&2; exit 1; }

trap 'fail "unexpected error at release.sh line $LINENO (last command: $BASH_COMMAND)"' ERR

VERSIONS_PLUGIN="org.codehaus.mojo:versions-maven-plugin:2.22.0"

set_version() {
  local output
  if ! output=$(mvn -B "${VERSIONS_PLUGIN}:set" -DnewVersion="$1" -DgenerateBackupPoms=false 2>&1); then
    echo "$output" >&2
    fail "could not set the version to $1 — see the Maven output above (a malformed version string is the usual cause)"
  fi
}

CURRENT_VERSION=$(mvn -q -Dexpression=project.version -DforceStdout help:evaluate)

RELEASE_VERSION="${1:-}"
if [ -z "$RELEASE_VERSION" ]; then
  case "$CURRENT_VERSION" in
    *-SNAPSHOT) RELEASE_VERSION="${CURRENT_VERSION%-SNAPSHOT}" ;;
    *) fail "Current version ($CURRENT_VERSION) is not a SNAPSHOT — pass the release version explicitly: ./release.sh 1.2.3" ;;
  esac
fi

NEXT_SNAPSHOT="${2:-}"
if [ -z "$NEXT_SNAPSHOT" ]; then
  if [[ "$RELEASE_VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    NEXT_SNAPSHOT="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.$((BASH_REMATCH[3] + 1))-SNAPSHOT"
  else
    fail "Can't auto-derive the next SNAPSHOT version from '$RELEASE_VERSION' (not MAJOR.MINOR.PATCH) — pass it explicitly: ./release.sh $RELEASE_VERSION 1.2.3-SNAPSHOT"
  fi
fi

echo "Current version:      $CURRENT_VERSION"
echo "Release version:      $RELEASE_VERSION"
echo "Next dev version:     $NEXT_SNAPSHOT"
read -r -p "Proceed? [y/N] " CONFIRM
[ "$CONFIRM" = "y" ] || [ "$CONFIRM" = "Y" ] || fail "Aborted by user"

log "1/3 — Setting version to $RELEASE_VERSION"
set_version "$RELEASE_VERSION"

log "2/3 — Running the gated deploy (build, Sonar, Dependency-Track, Nexus)"
if ! ./deploy.sh; then
  log "deploy.sh failed — see the \"FAILED: ...\" message printed above (right before this line) for the exact cause. Rolling pom.xml back to $CURRENT_VERSION."
  set_version "$CURRENT_VERSION"
  fail "Release $RELEASE_VERSION did NOT complete (scroll up to the \"FAILED: ...\" line from deploy.sh for why) — pom.xml restored to $CURRENT_VERSION, nothing was left half-released"
fi

log "3/3 — Bumping to the next development version: $NEXT_SNAPSHOT"
set_version "$NEXT_SNAPSHOT"

log "Released $RELEASE_VERSION. Working copy is now $NEXT_SNAPSHOT."
