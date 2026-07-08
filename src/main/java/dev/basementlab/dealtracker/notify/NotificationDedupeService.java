package dev.basementlab.dealtracker.notify;

import dev.basementlab.dealtracker.repository.NotificationEventRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class NotificationDedupeService {

    private final NotificationEventRepository repository;

    public NotificationDedupeService(NotificationEventRepository repository) {
        this.repository = repository;
    }

    public boolean shouldSend(long watchTargetId, long triggerRuleId, int cooldownHours) {
        Optional<Instant> lastSent = repository.findLatestSentAt(watchTargetId, triggerRuleId);
        return shouldSend(lastSent, cooldownHours, Instant.now());
    }

    // Pure function, no DB/clock dependency - this is the part actually worth unit testing.
    static boolean shouldSend(Optional<Instant> lastSent, int cooldownHours, Instant now) {
        return lastSent
                .map(sent -> Duration.between(sent, now).toHours() >= cooldownHours)
                .orElse(true); // never sent before -> fine to send
    }

    public void recordSent(long watchTargetId, Long listingId, long triggerRuleId, Long observationId) {
        Instant now = Instant.now();
        String eventKey = "%d:%d:%d".formatted(watchTargetId, triggerRuleId, now.toEpochMilli());
        repository.insert(watchTargetId, listingId, triggerRuleId, observationId, eventKey, now);
    }
}
