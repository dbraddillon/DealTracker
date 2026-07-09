#!/usr/bin/env bash
# Runs the Java app just long enough to complete one poll cycle against the real
# collector endpoints (e.g. gorillamind.com), then stops it. For refreshing the local
# data/dealtracker.db that viewer/ reads, without leaving the poller running in the
# background (PollingJob polls forever on a fixed delay - see @Scheduled in
# scheduler/PollingJob.java - this script just samples one cycle of it).
set -euo pipefail

cd "$(dirname "$0")"

export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"

mkdir -p data

count_observations() {
    if [ -f data/dealtracker.db ]; then
        sqlite3 data/dealtracker.db "SELECT COUNT(*) FROM observation;" 2>/dev/null || echo 0
    else
        echo 0
    fi
}

before=$(count_observations)
log=$(mktemp)

echo "Starting DealTracker for one poll cycle..."
mvn -q spring-boot:run > "$log" 2>&1 &
pid=$!

after="$before"
for _ in $(seq 1 30); do
    sleep 2
    after=$(count_observations)
    if [ "$after" -gt "$before" ]; then
        break
    fi
    if ! kill -0 "$pid" 2>/dev/null; then
        break
    fi
done

# spring-boot:run forks its own JVM under mvn - killing $pid alone can leave that JVM
# running, so sweep by pattern (same approach that reliably stopped it during setup).
pkill -f "spring-boot:run" 2>/dev/null || true
sleep 1

if [ "$after" -gt "$before" ]; then
    echo "Got $((after - before)) new observation(s) - total now $after."
    rm -f "$log"
else
    echo "No new observations recorded. App output:"
    cat "$log"
    rm -f "$log"
    exit 1
fi
