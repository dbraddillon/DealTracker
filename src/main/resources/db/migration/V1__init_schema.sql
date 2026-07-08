-- Core watch-engine schema. Kept generic per docs/DealTrackerconcept.md - nothing here is
-- product-specific; Gorilla Mind is seeded as data in V2, not modeled in the schema.

CREATE TABLE source (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('html', 'api', 'feed', 'manual')),
    base_url TEXT,
    active INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE watch_target (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category TEXT,
    brand TEXT,
    search_terms_json TEXT,
    notes TEXT,
    active INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE listing (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    watch_target_id INTEGER NOT NULL REFERENCES watch_target(id),
    source_id INTEGER NOT NULL REFERENCES source(id),
    url TEXT NOT NULL,
    external_key TEXT,
    variant_label TEXT,
    currency TEXT NOT NULL DEFAULT 'USD',
    active INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX idx_listing_watch_target ON listing(watch_target_id);
CREATE INDEX idx_listing_source ON listing(source_id);

CREATE TABLE observation (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    listing_id INTEGER NOT NULL REFERENCES listing(id),
    observed_at TEXT NOT NULL, -- ISO-8601 instant
    title TEXT,
    availability TEXT,
    price NUMERIC,
    sale_price NUMERIC,
    member_price NUMERIC,
    subscribe_price NUMERIC,
    quantity_value NUMERIC,
    quantity_unit TEXT,
    promo_text TEXT,
    raw_hash TEXT,
    meta_json TEXT
);
CREATE INDEX idx_observation_listing_time ON observation(listing_id, observed_at DESC);

CREATE TABLE trigger_rule (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    watch_target_id INTEGER NOT NULL REFERENCES watch_target(id),
    rule_type TEXT NOT NULL CHECK (rule_type IN (
        'price_below', 'price_per_unit_below', 'percent_drop', 'promo_match', 'back_in_stock'
    )),
    threshold_value NUMERIC,
    match_text TEXT,
    cooldown_hours INTEGER NOT NULL DEFAULT 24,
    active INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX idx_trigger_rule_watch_target ON trigger_rule(watch_target_id);

CREATE TABLE notification_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    watch_target_id INTEGER NOT NULL REFERENCES watch_target(id),
    listing_id INTEGER REFERENCES listing(id),
    trigger_rule_id INTEGER NOT NULL REFERENCES trigger_rule(id),
    observation_id INTEGER REFERENCES observation(id),
    event_key TEXT NOT NULL,
    sent_at TEXT NOT NULL
);
-- Dedupe/cooldown lookups always filter by (watch_target_id, trigger_rule_id) ordered by sent_at.
CREATE INDEX idx_notification_event_target_rule ON notification_event(watch_target_id, trigger_rule_id, sent_at DESC);
