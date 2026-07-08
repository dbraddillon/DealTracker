package dev.basementlab.dealtracker.collector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShopifyProduct(
        long id,
        String title,
        String handle,
        List<ShopifyVariant> variants
) {}
