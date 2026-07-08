# DealTracker

## What this is
A local-first "watch engine" that monitors retailer listings for price drops / deals and
emails an alert when a trigger rule fires. Not product-specific — a generic watch_target /
listing / observation / trigger_rule / notification_event model (see
`docs/DealTrackerconcept.md`), first seeded with Gorilla Mind energy drinks but designed to
track anything (supplements, tools, electronics, grill gear, etc.) without schema changes.

**Status: pre-implementation.** No code yet — this repo currently holds the concept doc only.
Stack, schema DDL, and scraping approach are not yet decided.

## Intended environment
Designed to run as a scheduled background job on the Mac Mini (`Mack`), the home LLM host.
See `../HomeServer/CLAUDE.md` (cloned sibling repo) for full network/infra reference:
- Mack SSH: `ssh -i ~/.ssh/id_ed25519 Mack@192.168.50.20` (LAN) or `Mack@100.102.185.102` (Tailscale)
- Mack is double-NAT — no inbound from the internet. Fine for a background poller with no
  public endpoint; if a UI/dashboard is added later, it needs to go through the basement
  server (k3s, `basementlab.dev`) reverse-proxied to Mack, same pattern as ComfyUI/ESPHome.
- Local Ollama available on Mack at `192.168.50.20:11434` if any LLM-assisted parsing
  (e.g. messy promo text extraction) is wanted later — see HomeServer CLAUDE.md "AI / Local
  LLM Stack" section for calling conventions.
- SQLite `.db` files placed under `/opt/<appname>/` on the **basement server** get nightly
  S3 backup automatically. Mack itself has no equivalent backup cron yet — worth deciding
  where the DB actually lives before relying on it for anything not easily re-seeded.

## Project structure
- `docs/DealTrackerconcept.md` — Perplexity-authored concept + schema writeup (generic watch
  engine design, trigger examples, Claude handoff prompt for building it out)

## Key constraints & priorities
- Keep the core schema generic (watch_target/source/listing/observation/trigger_rule/
  notification_event) — do not special-case Gorilla Mind or any single product in the schema.
- Local-first: SQLite, no cloud dependency required to run.
- Retailer scraping is the main open risk (ToS, anti-bot, brittleness) — not yet designed.

## Useful docs
- `docs/DealTrackerconcept.md` — schema design, trigger rule types, seed data intent
- `../HomeServer/CLAUDE.md` — Mac Mini specs, Tailscale/LAN addresses, Ollama endpoints, deploy patterns
