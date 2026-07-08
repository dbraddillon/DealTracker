package dev.basementlab.dealtracker.repository;

import dev.basementlab.dealtracker.domain.TriggerRule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TriggerRuleRepository {

    private static final RowMapper<TriggerRule> MAPPER = (rs, rowNum) -> new TriggerRule(
            rs.getLong("id"),
            rs.getLong("watch_target_id"),
            rs.getString("rule_type"),
            rs.getBigDecimal("threshold_value"),
            rs.getString("match_text"),
            rs.getInt("cooldown_hours"),
            rs.getBoolean("active")
    );

    private final JdbcTemplate jdbc;

    public TriggerRuleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TriggerRule> findActiveByWatchTarget(long watchTargetId) {
        return jdbc.query(
                "SELECT * FROM trigger_rule WHERE watch_target_id = ? AND active = 1",
                MAPPER,
                watchTargetId
        );
    }
}
