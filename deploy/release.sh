#!/usr/bin/env bash
# shellcheck disable=SC2029  # the variables are meant to expand on this side, into the remote command
# Upload target/app.jar to a new release directory on the server, switch the "current" symlink and restart
# the systemd unit. Used by the deploy jobs:  deploy/release.sh staging|production
#
# Layout on the server (created by the Ansible project):
#   /srv/java-systemd/<env>/releases/<pipeline id>/app.jar     one directory per deployment
#   /srv/java-systemd/<env>/current -> releases/<pipeline id>  what the systemd unit runs
set -euo pipefail

ENVIRONMENT="${1:?usage: release.sh staging|production}"
APP="${APP:-java-systemd}"
BUILD_ID="${CI_PIPELINE_ID:?CI_PIPELINE_ID is not set (run this from a pipeline)}"
KEEP="${KEEP_RELEASES:-5}"

BASE="/srv/${APP}/${ENVIRONMENT}"
RELEASE="${BASE}/releases/${BUILD_ID}"
UNIT="${APP}-${ENVIRONMENT}"
URL="https://${UNIT}.${EXAMPLES_DOMAIN:?EXAMPLES_DOMAIN is not set}"

echo "==> upload target/app.jar to ${RELEASE}"
ssh deploy "mkdir -p '${RELEASE}'"
rsync -az --partial --info=stats1 -e ssh target/app.jar "deploy:${RELEASE}/app.jar"

echo "==> switch the symlink (atomic rename) and restart ${UNIT}"
ssh deploy "set -e; ln -sfn '${RELEASE}' '${BASE}/current.new'; mv -T '${BASE}/current.new' '${BASE}/current'; sudo systemctl restart '${UNIT}'"

echo "==> wait for ${URL}"
# The unit needs a few seconds (Flyway migrates the database during startup). Until then Caddy answers 502/503.
curl -fsS -o /dev/null --retry 40 --retry-delay 3 --retry-all-errors "${URL}/actuator/health"
curl -fsS "${URL}/" | tee /dev/stderr | grep -q "\"build\":\"${BUILD_ID}\"" \
  || { echo; echo "ERROR: ${URL} is up but does not run build ${BUILD_ID}" >&2; exit 1; }
echo
echo "==> ${UNIT} runs build ${BUILD_ID}"

echo "==> keep the last ${KEEP} releases"
ssh deploy "cd '${BASE}/releases' && current=\$(readlink -f '${BASE}/current') && ls -1dt */ | tail -n +$((KEEP + 1)) | while read -r d; do [ \"\$(readlink -f \"\$d\")\" = \"\$current\" ] || rm -rf -- \"\$d\"; done; ls -1 ."
