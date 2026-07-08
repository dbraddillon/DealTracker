-- Seed data only - proves the generic model with one real target. Nothing about the schema
-- is Gorilla-Mind-specific; this is just the first row set.

INSERT INTO source (name, kind, base_url, active)
VALUES ('Gorilla Mind (Shopify)', 'api', 'https://gorillamind.com', 1);

INSERT INTO watch_target (name, category, brand, search_terms_json, notes, active)
VALUES (
    'Gorilla Mind Energy Drink (12 Pack)',
    'supplement',
    'Gorilla Mind',
    '["energy drink","gorilla mind energy"]',
    'First seed target. No external_key pinned yet - ShopifyJsonCollector matches products by these search terms against the collection JSON each poll.',
    1
);

-- external_key intentionally NULL: this listing isn't pinned to one Shopify product/variant ID
-- yet. ShopifyJsonCollector title-matches against watch_target.search_terms_json instead, per
-- the concept doc's "watch_target can exist before any listing is known" requirement.
INSERT INTO listing (watch_target_id, source_id, url, external_key, variant_label, currency, active)
VALUES (
    1,
    1,
    'https://gorillamind.com/collections/energy-drinks/products.json',
    NULL,
    NULL,
    'USD',
    1
);

INSERT INTO trigger_rule (watch_target_id, rule_type, threshold_value, cooldown_hours, active)
VALUES (1, 'price_below', 28.00, 24, 1);
