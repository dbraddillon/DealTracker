package dev.basementlab.dealtracker.rule;

import dev.basementlab.dealtracker.domain.Observation;
import dev.basementlab.dealtracker.domain.TriggerRule;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

// Strategy dispatch on rule_type, same shape as CollectorRegistry's dispatch on source.kind -
// deliberately consistent so both "extension points" (add a retailer, add a rule type) look
// the same to someone reading the code later.
@Component
public class RuleEvaluator {

    public RuleResult evaluate(TriggerRule rule, List<Observation> batch) {
        return switch (rule.ruleType()) {
            case "price_below" -> evaluatePriceBelow(rule, batch);
            // Stubbed - same interface as price_below, just always "doesn't fire" until built.
            // Adding one is a new case here, not a redesign of anything upstream.
            case "price_per_unit_below", "percent_drop", "promo_match", "back_in_stock" -> RuleResult.noFire();
            default -> throw new IllegalArgumentException("Unknown rule_type: " + rule.ruleType());
        };
    }

    private RuleResult evaluatePriceBelow(TriggerRule rule, List<Observation> batch) {
        // If more than one variant qualifies (e.g. two flavors both on sale), surface the
        // cheapest one - that's the deal actually worth telling Brad about.
        return batch.stream()
                .filter(o -> o.effectivePrice() != null)
                .filter(o -> o.effectivePrice().compareTo(rule.thresholdValue()) < 0)
                .min(Comparator.comparing(Observation::effectivePrice))
                .map(RuleResult::fire)
                .orElse(RuleResult.noFire());
    }
}
