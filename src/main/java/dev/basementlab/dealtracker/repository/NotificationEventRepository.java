package dev.basementlab.dealtracker.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class NotificationEventRepository {

    private final JdbcTemplate jdbc;

    public NotificationEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Optional<Instant> here plays the role C# would give a nullable DateTimeOffset? - "maybe
    // never sent before" is a real, expected case (first time this rule ever fires), not an error.
    public Optional<Instant> findLatestSentAt(long watchTargetId, long triggerRuleId) {
        try {
            String sentAt = jdbc.queryForObject(
                    """
                    SELECT sent_at FROM notification_event
                    WHERE watch_target_id = ? AND trigger_rule_id = ?
                    ORDER BY sent_at DESC LIMIT 1
                    """,
                    String.class,
                    watchTargetId,
                    triggerRuleId
            );
            return Optional.of(Instant.parse(sentAt));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void insert(long watchTargetId, Long listingId, long triggerRuleId, Long observationId,
                        String eventKey, Instant sentAt) {
        jdbc.update(
                """
                INSERT INTO notification_event
                    (watch_target_id, listing_id, trigger_rule_id, observation_id, event_key, sent_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                watchTargetId, listingId, triggerRuleId, observationId, eventKey, sentAt.toString()
        );
    }
}
