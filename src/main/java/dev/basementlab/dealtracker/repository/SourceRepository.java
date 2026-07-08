package dev.basementlab.dealtracker.repository;

import dev.basementlab.dealtracker.domain.Source;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

// @Repository is a @Component specialization that also enables Spring's exception translation
// (JDBC SQLExceptions -> DataAccessException hierarchy) - roughly like a scoped service class
// in .NET DI, but the translation part has no direct EF Core/Dapper equivalent.
@Repository
public class SourceRepository {

    private static final RowMapper<Source> MAPPER = (rs, rowNum) -> new Source(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("kind"),
            rs.getString("base_url"),
            rs.getBoolean("active")
    );

    private final JdbcTemplate jdbc;

    public SourceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Source findById(long id) {
        return jdbc.queryForObject("SELECT * FROM source WHERE id = ?", MAPPER, id);
    }
}
