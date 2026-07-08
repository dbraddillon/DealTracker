package dev.basementlab.dealtracker.domain;

// A Java record - like a C# positional record (`record Source(...)`), immutable and with
// equals/hashCode/toString generated for you. No separate class body needed unless you want
// extra methods.
public record Source(
        Long id,
        String name,
        String kind, // "html" | "api" | "feed" | "manual" - dispatch key for CollectorRegistry
        String baseUrl,
        boolean active
) {}
