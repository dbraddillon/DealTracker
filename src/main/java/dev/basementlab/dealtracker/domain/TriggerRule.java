package dev.basementlab.dealtracker.domain;

import java.math.BigDecimal;

public record TriggerRule(
        Long id,
        Long watchTargetId,
        String ruleType, // price_below | price_per_unit_below | percent_drop | promo_match | back_in_stock
        BigDecimal thresholdValue,
        String matchText,
        int cooldownHours,
        boolean active
) {}
