package dev.basementlab.dealtracker.rule;

import dev.basementlab.dealtracker.domain.Observation;
import dev.basementlab.dealtracker.domain.TriggerRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluatorTest {

    private final RuleEvaluator evaluator = new RuleEvaluator();

    @Test
    void priceBelowFiresWhenRegularPriceUnderThreshold() {
        TriggerRule rule = priceBelowRule(new BigDecimal("28.00"));
        List<Observation> batch = List.of(observation("25.00", null));

        RuleResult result = evaluator.evaluate(rule, batch);

        assertThat(result.fires()).isTrue();
    }

    @Test
    void priceBelowDoesNotFireWhenPriceAtOrAboveThreshold() {
        TriggerRule rule = priceBelowRule(new BigDecimal("28.00"));
        List<Observation> batch = List.of(observation("34.99", null));

        RuleResult result = evaluator.evaluate(rule, batch);

        assertThat(result.fires()).isFalse();
    }

    @Test
    void priceBelowFiresWhenOnSaleUnderThreshold() {
        TriggerRule rule = priceBelowRule(new BigDecimal("28.00"));
        List<Observation> batch = List.of(observation("34.99", "26.99"));

        RuleResult result = evaluator.evaluate(rule, batch);

        assertThat(result.fires()).isTrue();
        assertThat(result.observation().effectivePrice()).isEqualByComparingTo("26.99");
    }

    @Test
    void priceBelowPicksCheapestQualifyingVariantWhenMultipleFire() {
        TriggerRule rule = priceBelowRule(new BigDecimal("30.00"));
        List<Observation> batch = List.of(
                observation("34.99", "27.99"),
                observation("34.99", "24.99"),
                observation("34.99", null) // doesn't qualify - not on sale
        );

        RuleResult result = evaluator.evaluate(rule, batch);

        assertThat(result.fires()).isTrue();
        assertThat(result.observation().effectivePrice()).isEqualByComparingTo("24.99");
    }

    @Test
    void unimplementedRuleTypesNeverFire() {
        TriggerRule rule = new TriggerRule(1L, 1L, "percent_drop", new BigDecimal("20"), null, 24, true);
        List<Observation> batch = List.of(observation("10.00", null));

        RuleResult result = evaluator.evaluate(rule, batch);

        assertThat(result.fires()).isFalse();
    }

    private TriggerRule priceBelowRule(BigDecimal threshold) {
        return new TriggerRule(1L, 1L, "price_below", threshold, null, 24, true);
    }

    private Observation observation(String price, String salePrice) {
        return new Observation(
                null, 1L, Instant.now(), "Test Product", "in_stock",
                new BigDecimal(price), salePrice == null ? null : new BigDecimal(salePrice),
                null, null, null, null, null, null, null
        );
    }
}
