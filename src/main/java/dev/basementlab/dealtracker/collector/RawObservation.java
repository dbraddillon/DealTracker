package dev.basementlab.dealtracker.collector;

import java.math.BigDecimal;

// What a Collector hands back before persistence assigns it an id/listing_id/observed_at.
// Deliberately source-agnostic - every Collector implementation (Shopify JSON today, Jsoup/HTML
// or a feed parser later) normalizes to this same shape so the rest of the pipeline
// (persistence, rule evaluation) never needs to know which collector produced a row.
public record RawObservation(
        String title,
        String availability, // "in_stock" | "out_of_stock" | null
        BigDecimal price,
        BigDecimal salePrice,
        String promoText,
        String metaJson
) {}
