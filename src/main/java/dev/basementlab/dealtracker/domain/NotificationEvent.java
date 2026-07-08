package dev.basementlab.dealtracker.domain;

import java.time.Instant;

public record NotificationEvent(
        Long id,
        Long watchTargetId,
        Long listingId,
        Long triggerRuleId,
        Long observationId,
        String eventKey,
        Instant sentAt
) {}
