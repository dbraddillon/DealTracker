# DealTracker

## What this is
A local-first "watch engine" that polls retailer listings for price drops / deals and emails
an alert when a trigger rule fires. Not product-specific — a generic watch_target / source /
listing / observation / trigger_rule / notification_event model (see
`docs/DealTrackerconcept.md`), seeded first with Gorilla Mind energy drinks but designed to
track anything without schema changes.

**Status: v1 implemented.** Java 21 + Spring Boot, SQLite (Flyway-migrated), one collector
(Shopify JSON API), one rule type (`price_below`), SES email notifications, deployed as a
Docker container on the Mac Mini.

## Stack
- Java 21, Spring Boot 3.5, Maven (matches conventions already established in `../floci-java-sandbox`)
- SQLite via `org.xerial:sqlite-jdbc`, schema managed by Flyway (`src/main/resources/db/migration/`)
- Plain `JdbcTemplate` for persistence — no JPA/Hibernate; closest Spring equivalent to the
  Dapper-style raw-SQL approach used elsewhere
- AWS SDK v2 (`sesv2`) for email notifications
- `java.net.http.HttpClient` for outbound HTTP (no `spring-boot-starter-web` — this app has no
  inbound HTTP surface, so the embedded servlet container would be dead weight. Note: skipping
  that starter also means Spring's Jackson auto-configuration never fires — see `config/AppConfig.java`,
  which declares the `ObjectMapper` bean manually instead)

## Build & run
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # match this repo's target, not whatever `java` defaults to
export PATH="$JAVA_HOME/bin:$PATH"

mvn test              # unit tests only, no network/AWS required
mvn spring-boot:run   # runs locally against the real gorillamind.com endpoint; SQLite file at ./data/dealtracker.db
```
No AWS credentials are needed just to run/poll — only `SesNotificationSender` touches AWS, and
only when a rule actually fires.

## Project structure
```
domain/       plain records: WatchTarget, Source, Listing, Observation, TriggerRule, NotificationEvent
collector/    Collector interface + CollectorRegistry (dispatch on source.kind) + ShopifyJsonCollector
rule/         RuleEvaluator (dispatch on trigger_rule.rule_type) — only price_below implemented, rest stubbed
notify/       NotificationSender + SesNotificationSender + NotificationDedupeService (cooldown_hours logic)
scheduler/    PollingJob — the @Scheduled entry point tying everything together
repository/   JdbcTemplate repositories, one per table
config/       AppConfig (manual ObjectMapper bean), AwsConfig (SesV2Client bean)
```
Two extension points are deliberately symmetric: adding a retailer = new `Collector` bean;
adding a rule type = new case in `RuleEvaluator`. Neither requires touching `PollingJob`.

`ShopifyJsonCollector` doesn't scrape HTML — Shopify stores expose a public, unauthenticated
`/products.json` (or `/collections/<handle>/products.json`) endpoint by default. The seed
listing (`V2__seed_gorilla_mind.sql`) has `external_key = NULL` and matches products by
title against `watch_target.search_terms_json` each poll, rather than pinning to one product ID —
lets a watch_target exist before a specific listing is nailed down, per the concept doc's intent.

## How to deploy
Mac Mini ("Mack") only — no basement server / k3s involvement, no inbound traffic needed.
Runs as a plain Docker container with `--restart unless-stopped`.

```bash
./deploy-mack.sh
```
No env vars needed — credentials come from the `dealtracker-app` AWS CLI profile already
configured on Mack (see below), mounted read-only into the container.

**Image is built locally, not on Mack**, then shipped over with `docker save | ssh ... docker
load`. Originally planned to build natively on Mack like KrakenBot/SolanaSniper, but Mack's
own outbound Docker pulls hit a flaky egress path first observed 2026-07-08 — `docker pull`
failed with "network is unreachable" against *some* resolved registry/CDN IPs while a plain
`curl` to the same hostname succeeded (got a different IP from the same DNS round-robin pool).
Looks like a partial routing issue on Mack's network path, not a Docker/Colima config problem —
worth a look next time someone's touching the router, but building elsewhere and shipping the
finished image sidesteps it entirely. Both machines are ARM64, so no cross-compile concerns.

See `deploy-mack.sh` for the exact steps. See `../HomeServer/CLAUDE.md` for Mack SSH details
and network topology.

**Backup:** `backup-to-s3.sh` runs ON Mack via a nightly crontab entry (not yet installed —
add with `crontab -e` on Mack), copying the SQLite file to
`s3://voluntarytransactions-backups/dealtracker/`. Uses the same `dealtracker-app` AWS CLI
profile.

**AWS CLI + profile on Mack:** installed via `brew install awscli` (wasn't there before
2026-07-08). Profile configured with the scoped `dealtracker-app` IAM user's key — created
once, never re-displayed; if it needs rotating, generate a new access key via
`aws iam create-access-key --user-name dealtracker-app` and reconfigure the profile on Mack,
then deactivate/delete the old key.

## Key constraints & priorities
- Keep the core schema generic (watch_target/source/listing/observation/trigger_rule/
  notification_event) — do not special-case Gorilla Mind or any single product in the schema.
- Local-first: SQLite, no cloud dependency required to run the poll/collect/evaluate loop.
  AWS (SES + S3) is only for notification delivery and backup, not core operation.
- No secrets in code or git — the app reads AWS credentials from the `dealtracker-app` CLI
  profile mounted read-only into the container (see `deploy-mack.sh`), never hardcoded,
  never passed as a literal secret value in any command or committed anywhere.
- Public/portfolio sharing is a possible future goal — keep naming and config generic
  (no hardcoded product/personal details in code). ToS/legal review for scraping-based
  collectors (as opposed to the Shopify JSON API used today) is deferred until it's relevant.

## Useful docs
- `docs/DealTrackerconcept.md` — original schema design, trigger rule types, seed data intent
- `docs/data-viewer-plan.md` — planning note for a local, read-only, no-hosting data viewer
  (not built yet — pick up here next)
- `../HomeServer/CLAUDE.md` — Mac Mini specs, Tailscale/LAN addresses, Ollama endpoints, deploy patterns
- `../floci-java-sandbox/CLAUDE.md` — Java/Spring conventions and C#-parallel comment policy this repo follows
