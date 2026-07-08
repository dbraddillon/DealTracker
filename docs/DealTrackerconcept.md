<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# slight rework of data structure, make things even more generic as if we'll add other things to search for by name, and gorilla mind energy drinks are just the first test and input, maybe i do some other thing i would buy if it's on sale

Yes — I’d shift it from a “product price tracker” into a more generic **watch engine** with entities, offers, observations, and triggers, so Gorilla Mind energy is just one seeded watch target instead of something baked into the schema. SQLite is a good fit for this kind of normalized local app, and generic price-monitoring architectures usually separate collection, parsing, change detection, and alerting anyway. [^1][^2][^3]

## Generic model

Think in terms of:

- `watch_target`: “thing I care about.”
- `listing`: a specific retailer/source page that may represent that thing.
- `observation`: what was seen on a given run.
- `trigger_rule`: what counts as actionable.
- `notification_event`: what already got emailed.

That lets you track supplements now, then grills, tools, electronics, shoes, tickets, or random one-off products later without changing the core tables. [^2][^3]

## Revised schema

Use these core tables:

- `watch_target`
    - `id`
    - `name`
    - `category`
    - `brand`
    - `search_terms_json`
    - `notes`
    - `active`
- `source`
    - `id`
    - `name`
    - `kind` (`html`, `api`, `feed`, `manual`)
    - `base_url`
    - `active`
- `listing`
    - `id`
    - `watch_target_id`
    - `source_id`
    - `url`
    - `external_key`
    - `variant_label`
    - `currency`
    - `active`
- `observation`
    - `id`
    - `listing_id`
    - `observed_at`
    - `title`
    - `availability`
    - `price`
    - `sale_price`
    - `member_price`
    - `subscribe_price`
    - `quantity_value`
    - `quantity_unit`
    - `promo_text`
    - `raw_hash`
    - `meta_json`
- `trigger_rule`
    - `id`
    - `watch_target_id`
    - `rule_type` (`price_below`, `price_per_unit_below`, `percent_drop`, `promo_match`, `back_in_stock`)
    - `threshold_value`
    - `match_text`
    - `cooldown_hours`
    - `active`
- `notification_event`
    - `id`
    - `watch_target_id`
    - `listing_id`
    - `trigger_rule_id`
    - `observation_id`
    - `event_key`
    - `sent_at`

The key change is that `watch_target` is no longer “a product SKU table.” It is just a user-defined target with optional search terms and rules.

## Better flexibility

Two fields make this future-proof:

- `search_terms_json`: supports things like `["gorilla mind energy","energy drink","gorilla energy 12 pack"]`.
- `meta_json`: lets you stash retailer-specific details like servings, flavor, size, UPC, coupon code, bundle size, or seller name without schema churn. SQLite works well with flexible schema patterns when you keep your core relational structure and push edge attributes into JSON. [^1][^4]

You can also add a nullable `canonical_name` later if you start deduplicating multiple messy listings into one cleaner identity.

## Trigger examples

Examples:

- Gorilla Mind energy: `price_below = 28.00`
- Any protein powder: `price_per_unit_below = 1.25` with `quantity_unit = servings`
- DeWalt drill set: `promo_match = "bogo|buy one get one|free battery"`
- Random watch item: `percent_drop = 20`

That means the engine is not “price only.” It becomes a general “notify me when this is worth buying” service. [^3]

## Claude handoff update

Use this revised handoff text:

```text
Rework the data model to be generic. Do not model this as a supplement-specific or Gorilla-Mind-specific app.

Intent:
This is a local-first watch engine for things I may want to buy when there is a deal. Gorilla Mind energy drinks are only the first seed input. Later I may track other supplements, tools, electronics, appliances, grill accessories, or anything else by name.

Design the core around generic watch targets, source listings, observations, rules, and notifications.

Core concepts:
1. watch_target = a thing I care about by name/category/brand/search terms
2. source = a retailer or data source
3. listing = a specific page or external item at a source that may correspond to a watch target
4. observation = one snapshot collected from a listing at a point in time
5. trigger_rule = user-defined rule for what counts as actionable
6. notification_event = dedupe/history of already-sent alerts

Required behavior:
- Allow a watch target to have multiple listings across multiple retailers.
- Allow a watch target to exist before any listing is known, so the system can later search or attach listings.
- Support generic rules like:
  - price below threshold
  - price per unit below threshold
  - percent drop from recent baseline
  - promo text match
  - back in stock
- Support retailer-specific extra fields without constant schema migrations.
- Persist locally in SQLite.
- Keep the system generic enough that adding a new category of item does not require changing the main schema.

Data model to propose:
- watch_target
- source
- listing
- observation
- trigger_rule
- notification_event

Implementation requirements:
- Include suggested schema.
- Include indexes.
- Include example records for Gorilla Mind energy drinks as seed data only.
- Include examples for at least one non-supplement item to prove the model is generic.
- Use JSON columns sparingly for flexible metadata, while keeping the main model relational and queryable.
- Design for background execution on a Mac mini with scheduled polling and email alerts.

Output:
1. Updated architecture
2. Updated schema
3. Example seed data
4. Updated rule evaluation flow
5. Notes on future expansion
```


## One extra tweak

I’d also add an optional `watch_list` table later if you want grouping like:

- supplements
- smoker/grill gear
- home maintenance
- computer hardware

That gives you multiple themed shopping buckets without changing the engine itself. Your current use case still works fine without it, so I’d keep v1 lean. [^2]

If you want, I can now turn this into the **actual SQLite DDL plus sample seed inserts**.
<span style="display:none">[^10][^11][^12][^13][^14][^15][^5][^6][^7][^8][^9]</span>

<div align="center">⁂</div>

[^1]: https://sqleditor.online/blog/sqlite-schema-design-patterns

[^2]: https://www.sqliteforum.com/p/effective-schema-design-for-sqlite

[^3]: https://www.joinmassive.com/blog/price-monitoring-system

[^4]: https://pmil.me/posts/flexible-schema

[^5]: https://medium.com/@soumij11/building-a-comprehensive-stock-tracker-with-streamlit-sqlite-and-plotly-2c3d915b9cd4

[^6]: https://databasesample.com/database/sqlite-sample-database

[^7]: https://shopapis.com/guides/price-monitoring-pipeline

[^8]: https://dev.to/0012303/i-built-a-price-monitoring-system-in-30-minutes-heres-the-stack-all-free-apis-1cg6

[^9]: https://finedata.ai/blog/build-price-monitoring-tool/

[^10]: https://www.timestored.com/data/sample/sqlite

[^11]: https://www.dbpro.app/learn/sqlite/guides/list-tables

[^12]: https://thirdwatch.dev/blog/build-price-monitoring-dashboard-for-ecommerce

[^13]: https://sqlite-utils.datasette.io/_/downloads/en/3.9/pdf/

[^14]: https://www.sqlitetutorial.net/sqlite-sample-database/

[^15]: https://sqlite.org/schematab.html

