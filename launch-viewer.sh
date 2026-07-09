#!/usr/bin/env bash
# One command: refresh local data with one poll cycle, then launch the viewer and open it
# in the browser. This is the "just show me the deals" entry point - refresh-local-data.sh
# and `cd viewer && npm run dev` still work standalone if you only want one half of this.
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"

"$script_dir/refresh-local-data.sh"

cd "$script_dir/viewer"

if [ ! -d node_modules ]; then
    echo "Installing viewer dependencies (first run only)..."
    npm install
fi

# Vite blocks in the foreground once started, so the browser-open has to happen from a
# background poller racing it - open as soon as the dev server responds, whichever comes first.
(
    for _ in $(seq 1 30); do
        if curl -s -o /dev/null "http://localhost:5173"; then
            open "http://localhost:5173"
            break
        fi
        sleep 0.5
    done
) &

npm run dev
