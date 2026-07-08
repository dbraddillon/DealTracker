package dev.basementlab.dealtracker.notify;

import dev.basementlab.dealtracker.domain.TriggerRule;
import dev.basementlab.dealtracker.domain.WatchTarget;
import dev.basementlab.dealtracker.rule.RuleResult;

// One implementation for now (SES). Kept as an interface anyway since "how we notify" is the
// same kind of pluggable concern as "how we collect" - a Telegram or Slack sender later is a
// new implementation, not a rework of PollingJob.
public interface NotificationSender {
    void send(WatchTarget watchTarget, TriggerRule rule, RuleResult result);
}
