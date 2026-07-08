package dev.basementlab.dealtracker.repository;

import dev.basementlab.dealtracker.domain.Observation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Repository
public class ObservationRepository {

    private final SimpleJdbcInsert insert;

    public ObservationRepository(JdbcTemplate jdbc) {
        // SimpleJdbcInsert generates the INSERT for you and hands back the auto-increment id -
        // closer to Dapper's `ExecuteScalar` for `... RETURNING id` than to EF's tracked-entity
        // key population, but same end result.
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("observation")
                .usingGeneratedKeyColumns("id");
    }

    public Observation insert(Observation o) {
        Map<String, Object> params = new HashMap<>();
        params.put("listing_id", o.listingId());
        params.put("observed_at", o.observedAt().toString());
        params.put("title", o.title());
        params.put("availability", o.availability());
        params.put("price", o.price());
        params.put("sale_price", o.salePrice());
        params.put("member_price", o.memberPrice());
        params.put("subscribe_price", o.subscribePrice());
        params.put("quantity_value", o.quantityValue());
        params.put("quantity_unit", o.quantityUnit());
        params.put("promo_text", o.promoText());
        params.put("raw_hash", o.rawHash());
        params.put("meta_json", o.metaJson());

        Number generatedId = insert.executeAndReturnKey(params);
        return new Observation(
                generatedId.longValue(),
                o.listingId(),
                o.observedAt(),
                o.title(),
                o.availability(),
                o.price(),
                o.salePrice(),
                o.memberPrice(),
                o.subscribePrice(),
                o.quantityValue(),
                o.quantityUnit(),
                o.promoText(),
                o.rawHash(),
                o.metaJson()
        );
    }

    public static Observation forListing(Long listingId, Instant observedAt, String title,
                                          String availability, BigDecimal price, BigDecimal salePrice,
                                          String promoText, String metaJson) {
        return new Observation(
                null, listingId, observedAt, title, availability, price, salePrice,
                null, null, null, null, promoText, null, metaJson
        );
    }
}
