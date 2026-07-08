#!/usr/bin/env bash
set -euo pipefail

# Runs ON Mack (not the dev machine) via a nightly crontab entry. Copies the SQLite file to the
# existing voluntarytransactions-backups bucket under a dealtracker/ prefix - reuses the bucket
# already used for the basement server's nightly backups, just a new prefix, no new bucket.
#
# Expects the AWS CLI to have a `dealtracker-app` profile configured on Mack
# (`aws configure --profile dealtracker-app`) using the same scoped IAM user the app itself
# uses - not your personal dev profile.

DB_PATH="${DEALTRACKER_DB_PATH:-/Users/mack/dealtracker/data/dealtracker.db}"
BUCKET="s3://voluntarytransactions-backups/dealtracker"
DATE=$(date +%F)

if [[ ! -f "$DB_PATH" ]]; then
    echo "No DB found at $DB_PATH - skipping backup" >&2
    exit 0
fi

/opt/homebrew/bin/aws s3 cp "$DB_PATH" "$BUCKET/${DATE}-dealtracker.db" --profile dealtracker-app
