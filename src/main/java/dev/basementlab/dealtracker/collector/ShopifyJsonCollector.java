package dev.basementlab.dealtracker.collector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.basementlab.dealtracker.domain.Listing;
import dev.basementlab.dealtracker.domain.WatchTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// source.kind = "api" collector for Shopify storefronts. Shopify exposes a public,
// unauthenticated {base}/products.json (or {base}/collections/<handle>/products.json) endpoint
// by default on every store - no scraping, no anti-bot risk, just a documented JSON response.
@Component
public class ShopifyJsonCollector implements Collector {

    private static final Logger log = LoggerFactory.getLogger(ShopifyJsonCollector.class);

    // HttpClient instances are immutable and thread-safe once built - same rationale as reusing
    // a single C# HttpClient/IHttpClientFactory-issued instance rather than `new`-ing one per call.
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper;

    public ShopifyJsonCollector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String kind() {
        return "api";
    }

    @Override
    public List<RawObservation> collect(WatchTarget watchTarget, Listing listing) {
        List<String> searchTerms = parseSearchTerms(watchTarget.searchTermsJson());
        String body = fetchBody(listing.url());
        List<RawObservation> results = parseAndFilter(body, searchTerms);

        if (results.isEmpty()) {
            log.warn("ShopifyJsonCollector found no matching products for watch_target '{}' at {}",
                    watchTarget.name(), listing.url());
        }
        return results;
    }

    // Split out from collect() so the parsing/matching/mapping logic (the part actually worth
    // unit testing) can run against a fixture string without touching the network - only
    // fetchBody() below talks to Shopify.
    List<RawObservation> parseAndFilter(String responseBody, List<String> searchTerms) {
        ShopifyProductsResponse response;
        try {
            response = objectMapper.readValue(responseBody, ShopifyProductsResponse.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Shopify products.json response", e);
        }

        List<RawObservation> results = new ArrayList<>();
        for (ShopifyProduct product : response.products()) {
            if (!matches(product, searchTerms)) {
                continue;
            }
            for (ShopifyVariant variant : product.variants()) {
                results.add(toRawObservation(product, variant));
            }
        }
        return results;
    }

    private boolean matches(ShopifyProduct product, List<String> searchTerms) {
        String title = product.title() == null ? "" : product.title().toLowerCase(Locale.ROOT);
        for (String term : searchTerms) {
            if (title.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private RawObservation toRawObservation(ShopifyProduct product, ShopifyVariant variant) {
        String title = "Default Title".equals(variant.title())
                ? product.title()
                : product.title() + " — " + variant.title();

        String availability = variant.available() ? "in_stock" : "out_of_stock";

        // Shopify's `price` is what you'd actually pay right now; `compare_at_price` is the
        // struck-through "was" price shown only when on sale. Our schema models it the other
        // way round (price = regular, sale_price = discounted), so this flips the two fields.
        BigDecimal regularPrice = variant.price();
        BigDecimal salePrice = null;
        if (variant.compareAtPrice() != null && variant.compareAtPrice().compareTo(variant.price()) > 0) {
            regularPrice = variant.compareAtPrice();
            salePrice = variant.price();
        }

        String metaJson;
        try {
            metaJson = objectMapper.writeValueAsString(Map.of(
                    "shopify_product_id", product.id(),
                    "shopify_variant_id", variant.id()
            ));
        } catch (IOException e) {
            metaJson = null;
        }

        return new RawObservation(title, availability, regularPrice, salePrice, null, metaJson);
    }

    private List<String> parseSearchTerms(String searchTermsJson) {
        if (searchTermsJson == null || searchTermsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(searchTermsJson, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Invalid search_terms_json: " + searchTermsJson, e);
        }
    }

    private String fetchBody(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            // .send() blocks the calling thread until the response arrives - unlike C#'s
            // HttpClient where GetAsync/await is the idiomatic default, java.net.http.HttpClient
            // makes synchronous send() the plain method and sendAsync() the opt-in for a
            // CompletableFuture. Fine here since PollingJob runs one poll cycle at a time anyway.
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Shopify request failed: " + response.statusCode() + " for " + url);
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to fetch " + url, e);
        }
    }
}
