#!/usr/bin/env bash
set -euo pipefail

# Deploys DealTracker to the Mac Mini ("Mack"), following the same pattern already proven for
# KrakenBot/SolanaSniper: build natively on Mack (ARM64) rather than cross-compile from another
# machine, run as a plain Docker container with --restart unless-stopped, no k3s/basementlab.dev
# involvement since there's no inbound traffic to serve.
#
# Requires AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY for the scoped `dealtracker-app` IAM user
# to be exported in your shell before running this - never hardcoded here, never committed.

MACK_HOST="${MACK_HOST:-Mack@192.168.50.20}"
MACK_SSH_KEY="${MACK_SSH_KEY:-$HOME/.ssh/id_ed25519}"
REMOTE_DIR="/Users/mack/dealtracker-src"
DATA_DIR="/Users/mack/dealtracker/data"
AWS_REGION_VALUE="${AWS_REGION:-us-east-1}"
EMAIL_FROM="${DEALTRACKER_EMAIL_FROM:-noreply@voluntarytransactions.com}"
EMAIL_TO="${DEALTRACKER_EMAIL_TO:-d.brad.dillon@gmail.com}"

if [[ -z "${AWS_ACCESS_KEY_ID:-}" || -z "${AWS_SECRET_ACCESS_KEY:-}" ]]; then
    echo "AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY must be exported (the scoped dealtracker-app" \
         "IAM user's key, not your personal dev profile) before running this script." >&2
    exit 1
fi

echo "==> Packing source"
TARBALL=$(mktemp -t dealtracker-src.XXXXXX.tar.gz)
tar -czf "$TARBALL" pom.xml src Dockerfile

echo "==> Copying to Mack"
ssh -i "$MACK_SSH_KEY" "$MACK_HOST" "mkdir -p $REMOTE_DIR $DATA_DIR"
scp -i "$MACK_SSH_KEY" "$TARBALL" "$MACK_HOST:$REMOTE_DIR/src.tar.gz"
rm -f "$TARBALL"

echo "==> Building on Mack"
ssh -i "$MACK_SSH_KEY" "$MACK_HOST" bash -s <<REMOTE
set -euo pipefail
eval \$(/opt/homebrew/bin/brew shellenv)
cd "$REMOTE_DIR"
tar -xzf src.tar.gz
docker build -t dealtracker .
REMOTE

echo "==> Restarting container"
ssh -i "$MACK_SSH_KEY" "$MACK_HOST" bash -s <<REMOTE
set -euo pipefail
eval \$(/opt/homebrew/bin/brew shellenv)
docker rm -f dealtracker 2>/dev/null || true
docker run -d \
    --name dealtracker \
    --restart unless-stopped \
    -v "$DATA_DIR:/data" \
    -e AWS_ACCESS_KEY_ID="$AWS_ACCESS_KEY_ID" \
    -e AWS_SECRET_ACCESS_KEY="$AWS_SECRET_ACCESS_KEY" \
    -e AWS_REGION="$AWS_REGION_VALUE" \
    -e DEALTRACKER_EMAIL_FROM="$EMAIL_FROM" \
    -e DEALTRACKER_EMAIL_TO="$EMAIL_TO" \
    dealtracker
REMOTE

echo "==> Done. Tail logs with:"
echo "    ssh -i $MACK_SSH_KEY $MACK_HOST docker logs -f dealtracker"
