package dev.basementlab.dealtracker.collector;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Spring auto-injects every Collector bean into this List constructor param - equivalent to
// requesting IEnumerable<ICollector> from the C# DI container, except Spring does it by type
// match alone, no explicit multi-registration needed on the container-config side.
@Component
public class CollectorRegistry {

    private final Map<String, Collector> byKind;

    public CollectorRegistry(List<Collector> collectors) {
        this.byKind = collectors.stream()
                .collect(Collectors.toMap(Collector::kind, Function.identity()));
    }

    public Collector forKind(String kind) {
        Collector collector = byKind.get(kind);
        if (collector == null) {
            throw new IllegalArgumentException("No Collector registered for source.kind = " + kind);
        }
        return collector;
    }
}
