package dev.basementlab.dealtracker.rule;

import dev.basementlab.dealtracker.domain.Observation;

// observation is the specific row that caused the fire (e.g. the cheapest matching variant) -
// carried through so the notification and notification_event row both know exactly what was seen.
public record RuleResult(boolean fires, Observation observation) {

    public static RuleResult noFire() {
        return new RuleResult(false, null);
    }

    public static RuleResult fire(Observation observation) {
        return new RuleResult(true, observation);
    }
}
