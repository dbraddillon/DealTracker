# Data viewer — planning note

Not built yet. Captured here 2026-07-08 so we can pick this up (next session, maybe tomorrow)
without re-deriving it.

## Goal
A simple, local, **read-only** visual way to look at what DealTracker has collected — no
hosting, no new k3s/basementlab.dev deployment, no changes to the running app on Mack. Just
something to pull up when you want to eyeball price history or recent activity.

## What it should show (rough sketch)
- Per watch_target: current price by variant/flavor, price history over time (a simple
  line/sparkline per flavor would cover it), whether anything's currently under a trigger_rule
  threshold right now
- Recent `notification_event` rows — what's already fired, when, for which rule
- A plain table browse of raw `observation` history, mostly useful for sanity-checking a new
  collector's behavior (e.g. confirming search-term matching picked up the right products)

## Leading approach: static HTML + sql.js, no server at all
A single self-contained HTML file using [sql.js](https://sql.js.org/) (SQLite compiled to
WASM, runs entirely in-browser) with a plain `<input type="file">` to pick the `.db` file
directly off disk via the browser's File API — no fetch, no CORS, no local dev server, nothing
resembling "hosting." Charts/tables rendered with plain JS/canvas or a tiny charting lib.

Workflow: `scp` the current `dealtracker.db` down from Mack when you want to look (it's a
live SQLite file, mounted at `/Users/mack/dealtracker/data/dealtracker.db`), then open the
viewer HTML and pick that local copy. Fully decoupled from the running app — this never
touches DealTracker's own code, just reads the same file structure it already writes.

## Alternatives considered (for when we actually build it)
- **JavaFX/Swing desktop reader, plain JDBC** — more Java reps (fits the learning-mode angle),
  but JavaFX isn't bundled with the JDK since Java 11 and packaging/distribution is historically
  annoying for a "just open it" tool. Worth it only if the sql.js version feels too limited.
- **Plain CLI table dump (shell/sqlite3, or a small Java main)** — not visual, no charts, but
  trivial and works everywhere. Could be a quick v0 before the graphical version if we want
  something even faster to stand up.

> **OPEN:** does the sql.js viewer need periodic re-copying of the .db file to stay current, or
> is "copy it down whenever you want to check" good enough? (Leaning: good enough — this is a
> look-when-curious tool, not a live dashboard.)

> **OPEN:** any interest in a super basic chart (price over time per flavor) vs. just a sorted
> table? Table is far less work; decide when we actually build it.
