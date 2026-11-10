#!/usr/bin/env bash
# shellcheck disable=SC2029  # the variables are meant to expand on this side, into the remote command
# Point "current" at the release before the current one and restart the unit.  deploy/rollback.sh staging|production
set -euo pipefail

ENVIRONMENT="${1:?usage: rollback.sh staging|production}"
APP="${APP:-java-systemd}"
BASE="/srv/${APP}/${ENVIRONMENT}"
UNIT="${APP}-${ENVIRONMENT}"
URL="https://${UNIT}.${EXAMPLES_DOMAIN:?EXAMPLES_DOMAIN is not set}"

previous="$(ssh deploy "cd '${BASE}/releases' && current=\$(basename \"\$(readlink -f '${BASE}/current')\") && ls -1t | grep -v -x \"\$current\" | head -n 1")"
[ -n "${previous}" ] || { echo "no earlier release to go back to" >&2; exit 1; }

echo "==> roll back ${UNIT} to release ${previous}"
ssh deploy "set -e; ln -sfn '${BASE}/releases/${previous}' '${BASE}/current.new'; mv -T '${BASE}/current.new' '${BASE}/current'; sudo systemctl restart '${UNIT}'"
curl -fsS -o /dev/null --retry 40 --retry-delay 3 --retry-all-errors "${URL}/actuator/health"
curl -fsS "${URL}/"
echo
