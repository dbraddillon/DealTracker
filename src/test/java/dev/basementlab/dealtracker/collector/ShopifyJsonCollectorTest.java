package dev.basementlab.dealtracker.collector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Note: uses AssertJ (assertj-core), which spring-boot-starter-test pulls in transitively - no
// extra dependency needed. `assertThat(x).isEqualTo(y)` reads a lot like FluentAssertions in
// C#, which isn't a coincidence: FluentAssertions was explicitly modeled on AssertJ's API.
class ShopifyJsonCollectorTest {

    private final ShopifyJsonCollector collector = new ShopifyJsonCollector(new ObjectMapper());

    @Test
    void filtersToMatchingProductsOnly() throws IOException {
        String body = readFixture();

        List<RawObservation> results = collector.parseAndFilter(body, List.of("energy drink"));

        // 3 variants on the matching product, 0 from the non-matching "Gorilla Mode Pre-Workout"
        assertThat(results).hasSize(3);
        assertThat(results).extracting(RawObservation::title)
                .allMatch(title -> title.contains("Gorilla Mind Energy Drink"));
    }

    @Test
    void mapsCompareAtPriceToRegularPriceAndCurrentPriceToSalePrice() throws IOException {
        String body = readFixture();

        List<RawObservation> results = collector.parseAndFilter(body, List.of("energy drink"));

        RawObservation onSale = results.stream()
                .filter(r -> r.title().contains("Rainbow Sherbet"))
                .findFirst()
                .orElseThrow();

        // Shopify: price=26.99, compare_at_price=34.99 (this variant is on sale)
        assertThat(onSale.price()).isEqualByComparingTo(new BigDecimal("34.99"));
        assertThat(onSale.salePrice()).isEqualByComparingTo(new BigDecimal("26.99"));
    }

    @Test
    void leavesSalePriceNullWhenNotDiscounted() throws IOException {
        String body = readFixture();

        List<RawObservation> results = collector.parseAndFilter(body, List.of("energy drink"));

        RawObservation fullPrice = results.stream()
                .filter(r -> r.title().contains("Fruit Punch"))
                .findFirst()
                .orElseThrow();

        assertThat(fullPrice.price()).isEqualByComparingTo(new BigDecimal("34.99"));
        assertThat(fullPrice.salePrice()).isNull();
    }

    @Test
    void mapsAvailabilityFromShopifysAvailableFlag() throws IOException {
        String body = readFixture();

        List<RawObservation> results = collector.parseAndFilter(body, List.of("energy drink"));

        RawObservation outOfStock = results.stream()
                .filter(r -> r.title().contains("Red Gummy Fish"))
                .findFirst()
                .orElseThrow();

        assertThat(outOfStock.availability()).isEqualTo("out_of_stock");
    }

    private String readFixture() throws IOException {
        return Files.readString(Path.of("src/test/resources/fixtures/gorillamind-products.json"));
    }
}
