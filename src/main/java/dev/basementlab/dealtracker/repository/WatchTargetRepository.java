package dev.basementlab.dealtracker.repository;

import dev.basementlab.dealtracker.domain.WatchTarget;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WatchTargetRepository {

    private static final RowMapper<WatchTarget> MAPPER = (rs, rowNum) -> new WatchTarget(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getString("brand"),
            rs.getString("search_terms_json"),
            rs.getString("notes"),
            rs.getBoolean("active")
    );

    private final JdbcTemplate jdbc;

    public WatchTargetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<WatchTarget> findAllActive() {
        return jdbc.query("SELECT * FROM watch_target WHERE active = 1", MAPPER);
    }
}
