package dev.basementlab.dealtracker.collector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

// Mirrors (a subset of) Shopify's public storefront /products.json variant shape.
// @JsonIgnoreProperties(ignoreUnknown = true) is required - Shopify's real payload has many
// more fields (sku, weight, images, options...) we don't care about; without this, Jackson's
// default is to fail deserialization on any field without a matching property (roughly the
// opposite of System.Text.Json/Newtonsoft's default of silently ignoring extras).
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShopifyVariant(
        long id,
        String title,
        BigDecimal price,
        // Shopify's compare_at_price is the pre-discount "was" price, only non-null when the
        // variant is actually on sale. It is NOT the current price - see effectivePrice mapping
        // in ShopifyJsonCollector for how this flips onto our price/sale_price columns.
        @JsonProperty("compare_at_price") BigDecimal compareAtPrice,
        boolean available
) {}
