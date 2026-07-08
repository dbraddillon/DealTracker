package dev.basementlab.dealtracker.domain;

import java.math.BigDecimal;
import java.time.Instant;

// BigDecimal for money, not double - same reasoning as C# `decimal` vs `double` for currency;
// Java just doesn't have a dedicated decimal primitive/keyword, BigDecimal is the class-based
// equivalent everywhere real money arithmetic matters.
public record Observation(
        Long id,
        Long listingId,
        Instant observedAt,
        String title,
        String availability, // "in_stock" | "out_of_stock" | null if unknown
        BigDecimal price, // regular/list price
        BigDecimal salePrice, // current discounted price if on sale, else null
        BigDecimal memberPrice,
        BigDecimal subscribePrice,
        BigDecimal quantityValue,
        String quantityUnit,
        String promoText,
        String rawHash,
        String metaJson
) {
    // The price a trigger_rule should actually compare against - whatever you'd pay right now.
    public BigDecimal effectivePrice() {
        return salePrice != null ? salePrice : price;
    }
}
