#!/usr/bin/env bash
set -euo pipefail

status="${NOTIFICATION_STATUS:-unknown}"
workflow="${NOTIFICATION_WORKFLOW:-GitHub Actions}"
job="${NOTIFICATION_JOB:-job}"
suite="${NOTIFICATION_SUITE:-n/a}"
repository="${NOTIFICATION_REPOSITORY:-unknown/repository}"
branch="${NOTIFICATION_BRANCH:-unknown}"
sha="${NOTIFICATION_SHA:-unknown}"
actor="${NOTIFICATION_ACTOR:-unknown}"
run_url="${NOTIFICATION_RUN_URL:-}"
artifacts_url="${NOTIFICATION_ARTIFACTS_URL:-${run_url}}"

if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
  echo "::add-mask::${SLACK_WEBHOOK_URL}"
fi

short_sha="${sha:0:7}"
title="[${status}] ${workflow} / ${job}"
summary="${title} - ${repository}@${branch} (${short_sha})"

details="Repository: ${repository}
Workflow: ${workflow}
Job: ${job}
Suite: ${suite}
Status: ${status}
Branch: ${branch}
Commit: ${short_sha}
Actor: ${actor}
Run: ${run_url}
Artifacts: ${artifacts_url}"

case "${status}" in
  success)
    color="2EB67D"
    ;;
  failure|cancelled)
    color="D40E0D"
    ;;
  *)
    color="ECB22E"
    ;;
esac

if [ -z "${SLACK_WEBHOOK_URL:-}" ]; then
  echo "Skipping Slack: SLACK_WEBHOOK_URL secret is not configured."
  exit 0
fi

payload="$(mktemp "${RUNNER_TEMP:-/tmp}/slack-payload.XXXXXX.json")"
trap 'rm -f "${payload}"' EXIT

TITLE="${title}" \
SUMMARY="${summary}" \
DETAILS="${details}" \
COLOR="${color}" \
RUN_URL="${run_url}" \
python3 - <<'PY' > "${payload}"
import json
import os

payload = {
    "text": os.environ["SUMMARY"],
    "attachments": [
        {
            "color": "#" + os.environ["COLOR"],
            "title": os.environ["TITLE"],
            "title_link": os.environ["RUN_URL"],
            "text": os.environ["DETAILS"],
            "mrkdwn_in": ["text"],
        }
    ],
}

print(json.dumps(payload))
PY

if curl --fail --silent --show-error --retry 2 --retry-delay 5 \
  --retry-all-errors \
  --connect-timeout 10 \
  --max-time 30 \
  -H "Content-Type: application/json" \
  --data @"${payload}" \
  "${SLACK_WEBHOOK_URL}" >/dev/null; then
  echo "Sent Slack notification."
else
  echo "::warning::Failed to send Slack notification."
fi
