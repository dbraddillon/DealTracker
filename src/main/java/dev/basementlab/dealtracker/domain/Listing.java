package dev.basementlab.dealtracker.domain;

public record Listing(
        Long id,
        Long watchTargetId,
        Long sourceId,
        String url,
        String externalKey, // nullable - pin to a specific product/variant once known; null means "search by watch_target's terms every poll"
        String variantLabel,
        String currency,
        boolean active
) {}
