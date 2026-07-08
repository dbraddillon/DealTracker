package dev.basementlab.dealtracker.repository;

import dev.basementlab.dealtracker.domain.Listing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ListingRepository {

    private static final RowMapper<Listing> MAPPER = (rs, rowNum) -> new Listing(
            rs.getLong("id"),
            rs.getLong("watch_target_id"),
            rs.getLong("source_id"),
            rs.getString("url"),
            rs.getString("external_key"),
            rs.getString("variant_label"),
            rs.getString("currency"),
            rs.getBoolean("active")
    );

    private final JdbcTemplate jdbc;

    public ListingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Listing> findActiveByWatchTarget(long watchTargetId) {
        return jdbc.query(
                "SELECT * FROM listing WHERE watch_target_id = ? AND active = 1",
                MAPPER,
                watchTargetId
        );
    }
}
