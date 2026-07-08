package dev.basementlab.dealtracker.domain;

public record WatchTarget(
        Long id,
        String name,
        String category,
        String brand,
        String searchTermsJson, // JSON array string, e.g. ["gorilla mind energy","energy drink"]
        String notes,
        boolean active
) {}
