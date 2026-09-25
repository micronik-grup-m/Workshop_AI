#!/usr/bin/env bash
# Build -> SonarQube analysis + quality gate -> SBOM upload to Dependency-Track
# + vulnerability gate -> deploy to Nexus. Stops (non-zero exit) at the first
# gate that fails, so nothing bad ever reaches Nexus.
#
# Required environment variables:
#   SONAR_TOKEN      - a SonarQube user token (My Account -> Security -> Generate Token)
# Optional environment variables (sensible defaults shown):
#   SONAR_HOST_URL    (default: https://sonar.georgesand.ro)
#   DTRACK_URL        (default: https://dtrack.georgesand.ro)
#   DTRACK_API_KEY    (default: read from the "dependency-track.apiKey" Maven
#                      property already set in ~/.m2/settings.xml)
#   DTRACK_MAX_CRITICAL  (default: 0)   - fail if Dependency-Track reports more
#   DTRACK_MAX_HIGH      (default: 0)   - fail if Dependency-Track reports more
#   DRY_RUN           (default: unset)  - set to "1" to run every check but
#                                         skip the final `mvn deploy`
#
# The NVD API key (for OWASP Dependency-Check, run as part of `mvn verify`)
# is read automatically from the "nvd" server entry in ~/.m2/settings.xml
# (pom.xml's dependency-check-maven plugin has nvdApiServerId=nvd) - nothing
# to pass here for that one.

SONAR_TOKEN="sqa_da3430e362feb1b69a18f20e0029334007eb48ba"

set -uo pipefail

log() { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
fail() { printf '\033[1;31mFAILED: %s\033[0m\n' "$1" >&2; exit 1; }
json_get() { python3 -c "import json,sys; d=json.load(sys.stdin); print(d$1)"; }

# Catch any command that isn't explicitly checked below (a stray typo, a
# missing tool, etc.) and say exactly which line/command it was, instead of
# a bare non-zero exit with no explanation.
trap 'fail "unexpected error at deploy.sh line $LINENO (last command: $BASH_COMMAND)"' ERR

# Runs an mvn build; on failure, prints the reason and the last part of the
# Maven output (the actual error is usually near the end) instead of just
# letting a bare exit code propagate.
run_mvn() {
  local step_name="$1"
  shift
  local log_file
  log_file=$(mktemp)
  if ! mvn "$@" 2>&1 | tee "$log_file"; then
    echo "----- last 40 lines of Maven output ($step_name) -----" >&2
    tail -40 "$log_file" >&2
    rm -f "$log_file"
    fail "$step_name failed — see the Maven output above for the exact cause (a failing test, a Checkstyle/PMD violation, or a Dependency-Check CVSS gate are the usual culprits for the build step; for the deploy step, check Nexus credentials/connectivity, or that this exact version wasn't already deployed — Nexus release repos normally reject re-deploying an existing release version)"
  fi
  rm -f "$log_file"
}

# curl wrapper that shows the HTTP status and response body on failure,
# instead of curl -f's silent, contentless failure.
curl_json() {
  local response http_status body
  response=$(curl -s -w '\n%{http_code}' "$@")
  http_status=$(echo "$response" | tail -1)
  body=$(echo "$response" | sed '$d')
  if [ "$http_status" -lt 200 ] || [ "$http_status" -ge 300 ]; then
    fail "HTTP $http_status calling '$*' — response body: ${body:-<empty>}"
  fi
  echo "$body"
}

: "${SONAR_TOKEN:?Set SONAR_TOKEN (SonarQube user token) before running this script}"

SONAR_HOST_URL="${SONAR_HOST_URL:-https://sonar.georgesand.ro}"
DTRACK_URL="${DTRACK_URL:-https://dtrack.georgesand.ro}"
DTRACK_MAX_CRITICAL="${DTRACK_MAX_CRITICAL:-0}"
DTRACK_MAX_HIGH="${DTRACK_MAX_HIGH:-0}"

cd "$(dirname "$0")"

if [ -z "${DTRACK_API_KEY:-}" ]; then
  DTRACK_API_KEY=$(mvn -q -Dexpression=dependency-track.apiKey -DforceStdout help:evaluate)
fi
[ -n "$DTRACK_API_KEY" ] || fail "DTRACK_API_KEY not set and not found in ~/.m2/settings.xml"

PROJECT_NAME=$(mvn -q -Dexpression=project.artifactId -DforceStdout help:evaluate)
PROJECT_VERSION=$(mvn -q -Dexpression=project.version -DforceStdout help:evaluate)
log "Deploying $PROJECT_NAME:$PROJECT_VERSION"

# ---------------------------------------------------------------------------
log "1/4 — Build, test, Checkstyle/PMD/Dependency-Check, SBOM generation, Sonar analysis"
# ---------------------------------------------------------------------------
run_mvn "Build/verify/Sonar step" -B clean verify sonar:sonar \
  -Dsonar.host.url="$SONAR_HOST_URL" \
  -Dsonar.token="$SONAR_TOKEN"

# ---------------------------------------------------------------------------
log "2/4 — Waiting for SonarQube's quality gate"
# ---------------------------------------------------------------------------
REPORT_TASK="target/sonar/report-task.txt"
[ -f "$REPORT_TASK" ] || fail "no $REPORT_TASK — did the sonar:sonar goal run?"
CE_TASK_URL=$(grep '^ceTaskUrl=' "$REPORT_TASK" | cut -d= -f2-)

ANALYSIS_ID=""
for _ in $(seq 1 60); do
  TASK_JSON=$(curl_json -u "${SONAR_TOKEN}:" "$CE_TASK_URL")
  STATUS=$(echo "$TASK_JSON" | json_get "['task']['status']")
  case "$STATUS" in
    SUCCESS)
      ANALYSIS_ID=$(echo "$TASK_JSON" | json_get "['task']['analysisId']")
      break
      ;;
    PENDING|IN_PROGRESS)
      sleep 5
      ;;
    *)
      fail "SonarQube analysis task ended with status $STATUS (task JSON: $TASK_JSON) — check $CE_TASK_URL"
      ;;
  esac
done
[ -n "$ANALYSIS_ID" ] || fail "SonarQube analysis task did not finish within 5 minutes — check $CE_TASK_URL"

GATE_JSON=$(curl_json -u "${SONAR_TOKEN}:" \
  "$SONAR_HOST_URL/api/qualitygates/project_status?analysisId=$ANALYSIS_ID")
GATE_STATUS=$(echo "$GATE_JSON" | json_get "['projectStatus']['status']")
echo "Quality gate status: $GATE_STATUS"
DASHBOARD_URL="$SONAR_HOST_URL/dashboard?id=$(mvn -q -Dexpression=project.groupId -DforceStdout help:evaluate):$PROJECT_NAME"
echo "Dashboard: $DASHBOARD_URL"
if [ "$GATE_STATUS" != "OK" ]; then
  FAILED_CONDITIONS=$(echo "$GATE_JSON" | python3 -c "
import json, sys
d = json.load(sys.stdin)
for c in d['projectStatus'].get('conditions', []):
    if c.get('status') != 'OK':
        print(f\"  - {c.get('metricKey')}: actual={c.get('actualValue')} (needed {c.get('comparator')} {c.get('errorThreshold')})\")
")
  fail "SonarQube quality gate is $GATE_STATUS. Failing condition(s):
$FAILED_CONDITIONS
See $DASHBOARD_URL for details."
fi

# ---------------------------------------------------------------------------
log "3/4 — Uploading SBOM to Dependency-Track and checking for vulnerabilities"
# ---------------------------------------------------------------------------
BOM_FILE="target/classes/META-INF/sbom/application.cdx.json"
[ -f "$BOM_FILE" ] || fail "no $BOM_FILE — did the cyclonedx-maven-plugin run?"

UPLOAD_RESPONSE=$(curl_json -X POST "$DTRACK_URL/api/v1/bom" \
  -H "X-Api-Key: $DTRACK_API_KEY" \
  -F "autoCreate=true" \
  -F "projectName=$PROJECT_NAME" \
  -F "projectVersion=$PROJECT_VERSION" \
  -F "bom=@${BOM_FILE};type=application/json")
PROJECT_UUID=$(echo "$UPLOAD_RESPONSE" | json_get "['projectUuid']")
BOM_TOKEN=$(echo "$UPLOAD_RESPONSE" | json_get "['token']")
echo "Dependency-Track project: $DTRACK_URL/projects/$PROJECT_UUID"

PROCESSING=""
for _ in $(seq 1 60); do
  PROCESSING=$(curl_json "$DTRACK_URL/api/v1/bom/token/$BOM_TOKEN" \
    -H "X-Api-Key: $DTRACK_API_KEY" | json_get "['processing']")
  [ "$PROCESSING" = "False" ] && break
  sleep 5
done
[ "$PROCESSING" = "False" ] || fail "Dependency-Track did not finish processing the BOM within 5 minutes — check $DTRACK_URL/projects/$PROJECT_UUID"

# Force a metrics recalculation, then give it a moment to persist.
curl_json "$DTRACK_URL/api/v1/metrics/project/$PROJECT_UUID/refresh" \
  -H "X-Api-Key: $DTRACK_API_KEY" > /dev/null
sleep 5

METRICS=$(curl_json "$DTRACK_URL/api/v1/metrics/project/$PROJECT_UUID/current" \
  -H "X-Api-Key: $DTRACK_API_KEY")
CRITICAL=$(echo "$METRICS" | json_get "['critical']")
HIGH=$(echo "$METRICS" | json_get "['high']")
MEDIUM=$(echo "$METRICS" | json_get "['medium']")
LOW=$(echo "$METRICS" | json_get "['low']")
POLICY_FAIL=$(echo "$METRICS" | json_get "['policyViolationsFail']")
echo "Findings — critical: $CRITICAL, high: $HIGH, medium: $MEDIUM, low: $LOW, policy violations (fail-level): $POLICY_FAIL"

if [ "$CRITICAL" -gt "$DTRACK_MAX_CRITICAL" ] || [ "$HIGH" -gt "$DTRACK_MAX_HIGH" ] || [ "$POLICY_FAIL" -gt 0 ]; then
  FINDINGS_LIST=$(curl_json "$DTRACK_URL/api/v1/finding/project/$PROJECT_UUID" \
    -H "X-Api-Key: $DTRACK_API_KEY" | python3 -c "
import json, sys
for f in json.load(sys.stdin):
    comp = f.get('component', {})
    vuln = f.get('vulnerability', {})
    sev = vuln.get('severity')
    if sev in ('CRITICAL', 'HIGH'):
        print(f\"  - {comp.get('name')} {comp.get('version')}: {vuln.get('vulnId')} ({sev})\")
")
  fail "Dependency-Track gate failed (limits: critical<=$DTRACK_MAX_CRITICAL, high<=$DTRACK_MAX_HIGH, policy violations must be 0).
Critical/High findings:
$FINDINGS_LIST
See $DTRACK_URL/projects/$PROJECT_UUID for the full list, including how to fix (usually: bump the affected dependency's version in pom.xml)."
fi
echo "Dependency-Track gate: OK"

# ---------------------------------------------------------------------------
if [ "${DRY_RUN:-}" = "1" ]; then
  log "DRY_RUN=1 — all gates passed, skipping the actual deploy"
  exit 0
fi

log "4/4 — Deploying $PROJECT_NAME:$PROJECT_VERSION to Nexus"
# ---------------------------------------------------------------------------
run_mvn "Nexus deploy" -B deploy -DskipTests \
  -Dcheckstyle.skip=true -Dpmd.skip=true -Dspotbugs.skip=true \
  -Ddependency-check.skip=true -Dsonar.skip=true -Djacoco.skip=true \
  -Dcyclonedx.skip=true

log "Done — $PROJECT_NAME:$PROJECT_VERSION deployed"
