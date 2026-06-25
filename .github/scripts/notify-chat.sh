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

payload_file() {
  local provider="$1"
  local output="$2"

  PROVIDER="${provider}" \
  TITLE="${title}" \
  SUMMARY="${summary}" \
  DETAILS="${details}" \
  COLOR="${color}" \
  RUN_URL="${run_url}" \
  python3 - <<'PY' > "${output}"
import json
import os

provider = os.environ["PROVIDER"]
title = os.environ["TITLE"]
summary = os.environ["SUMMARY"]
details = os.environ["DETAILS"]
color = os.environ["COLOR"]
run_url = os.environ["RUN_URL"]

if provider == "slack":
    payload = {
        "text": summary,
        "attachments": [
            {
                "color": "#" + color,
                "title": title,
                "title_link": run_url,
                "text": details,
                "mrkdwn_in": ["text"]
            }
        ],
    }
elif provider == "google-chat":
    payload = {"text": summary + "\n\n" + details}
elif provider == "teams":
    payload = {
        "@type": "MessageCard",
        "@context": "https://schema.org/extensions",
        "summary": summary,
        "themeColor": color,
        "title": title,
        "text": details.replace("\n", "\n\n"),
        "potentialAction": [
            {
                "@type": "OpenUri",
                "name": "Open GitHub run",
                "targets": [
                    {
                        "os": "default",
                        "uri": run_url
                    }
                ],
            }
        ],
    }
else:
    raise SystemExit("Unsupported provider: " + provider)

print(json.dumps(payload))
PY
}

post_webhook() {
  local provider="$1"
  local url="$2"

  if [ -z "${url}" ]; then
    echo "Skipping ${provider}: webhook secret is not configured."
    return 0
  fi

  local file="${RUNNER_TEMP:-/tmp}/${provider}-payload-${GITHUB_RUN_ID:-local}.json"
  payload_file "${provider}" "${file}"

  if curl --fail --silent --show-error --retry 2 --retry-delay 5 \
    -H "Content-Type: application/json" \
    --data @"${file}" \
    "${url}" >/dev/null; then
    echo "Sent ${provider} notification."
  else
    echo "::warning::Failed to send ${provider} notification."
  fi
}

post_webhook "slack" "${SLACK_WEBHOOK_URL:-}"
post_webhook "google-chat" "${GOOGLE_CHAT_WEBHOOK_URL:-}"
post_webhook "teams" "${TEAMS_WEBHOOK_URL:-}"
