package dev.basementlab.dealtracker.collector;

import dev.basementlab.dealtracker.domain.Listing;
import dev.basementlab.dealtracker.domain.WatchTarget;

import java.util.List;

// Strategy interface - one implementation per source.kind ("api", "html", "feed", "manual").
// Like a C# `interface ICollector` with multiple implementations resolved by a keyed lookup;
// Spring's equivalent of DI-by-key is CollectorRegistry building a Map<String, Collector> from
// every Collector bean's declared kind() rather than named/keyed service registration.
public interface Collector {

    // Which source.kind this collector handles - "api", "html", "feed", "manual".
    String kind();

    // watchTarget is passed alongside listing because some collectors (Shopify JSON today)
    // don't have a listing pinned to one exact product yet - they search the catalog using
    // watchTarget.searchTermsJson and can return zero, one, or several matching RawObservations
    // (e.g. one per flavor/variant) in a single poll.
    List<RawObservation> collect(WatchTarget watchTarget, Listing listing);
}
