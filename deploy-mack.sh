#!/usr/bin/env bash
set -euo pipefail

# Deploys DealTracker to the Mac Mini ("Mack"). Builds the image on THIS machine (also ARM64,
# so no cross-compile concerns) and ships the built image over via `docker save | ssh docker
# load`, rather than building on Mack itself - Mack's own outbound Docker pulls hit a flaky
# egress path (some resolved registry/CDN IPs are unreachable, others aren't - a network-level
# issue independent of DealTracker, first seen 2026-07-08). Building elsewhere and shipping the
# finished image sidesteps it entirely. Runs as a plain Docker container with
# --restart unless-stopped - no k3s/basementlab.dev involvement, no inbound traffic to serve.
#
# Credentials: the scoped `dealtracker-app` IAM user's key must already be configured as an
# AWS CLI profile named `dealtracker-app` ON MACK ITSELF (`aws configure --profile
# dealtracker-app`, run once, directly on Mack). This script mounts that profile's
# ~/.aws/credentials into the container read-only and sets AWS_PROFILE - the raw key is never
# passed through `docker run`, never appears in this script's arguments, and doesn't need to
# be known by whoever runs this deploy script.

MACK_HOST="${MACK_HOST:-Mack@192.168.50.20}"
MACK_SSH_KEY="${MACK_SSH_KEY:-$HOME/.ssh/id_ed25519}"
DATA_DIR="/Users/mack/dealtracker/data"
AWS_REGION_VALUE="${AWS_REGION:-us-east-1}"
EMAIL_FROM="${DEALTRACKER_EMAIL_FROM:-noreply@voluntarytransactions.com}"
EMAIL_TO="${DEALTRACKER_EMAIL_TO:-d.brad.dillon@gmail.com}"

echo "==> Building locally"
docker build -t dealtracker:latest .

echo "==> Shipping image to Mack"
docker save dealtracker:latest | ssh -i "$MACK_SSH_KEY" "$MACK_HOST" \
    'eval $(/opt/homebrew/bin/brew shellenv) && docker load'

echo "==> Restarting container"
ssh -i "$MACK_SSH_KEY" "$MACK_HOST" bash -s <<REMOTE
set -euo pipefail
eval \$(/opt/homebrew/bin/brew shellenv)
mkdir -p "$DATA_DIR"
docker rm -f dealtracker 2>/dev/null || true
docker run -d \
    --name dealtracker \
    --restart unless-stopped \
    -v "$DATA_DIR:/data" \
    -v "/Users/mack/.aws:/root/.aws:ro" \
    -e AWS_PROFILE="dealtracker-app" \
    -e AWS_REGION="$AWS_REGION_VALUE" \
    -e DEALTRACKER_EMAIL_FROM="$EMAIL_FROM" \
    -e DEALTRACKER_EMAIL_TO="$EMAIL_TO" \
    dealtracker:latest
REMOTE

echo "==> Done. Tail logs with:"
echo "    ssh -i $MACK_SSH_KEY $MACK_HOST docker logs -f dealtracker"
