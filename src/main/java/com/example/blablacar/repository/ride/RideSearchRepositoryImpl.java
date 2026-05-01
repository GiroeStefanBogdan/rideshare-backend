package com.example.blablacar.repository.ride;

import com.example.blablacar.dto.ride.RideSearchRequestDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class RideSearchRepositoryImpl implements RideSearchRepository {

    private static final double DEFAULT_MAX_DISTANCE_KM = 30.0;
    private static final double METERS_PER_KILOMETER = 1000.0;
    private static final ZoneId SEARCH_DAY_ZONE = ZoneId.systemDefault();

    private static final String BASE_SEARCH_SQL = """
            SELECT r.id AS ride_id,
                   rs_from.id AS rs_from_id,
                   rs_to.id AS rs_to_id,
                   ST_DistanceSphere(
                       ST_MakePoint(
                           COALESCE(s_from.longitude, a_from.longitude),
                           COALESCE(s_from.latitude, a_from.latitude)
                       ),
                       ST_MakePoint(:fromLon, :fromLat)
                   ) / 1000.0 AS dist_start_km,
                   ST_DistanceSphere(
                       ST_MakePoint(
                           COALESCE(s_to.longitude, a_to.longitude),
                           COALESCE(s_to.latitude, a_to.latitude)
                       ),
                       ST_MakePoint(:toLon, :toLat)
                   ) / 1000.0 AS dist_end_km
            FROM ride r
            JOIN ride_stop rs_from ON r.id = rs_from.ride_id
            JOIN ride_stop rs_to ON r.id = rs_to.ride_id
            JOIN admin_units a_from ON rs_from.location_id = a_from.id
            JOIN admin_units a_to ON rs_to.location_id = a_to.id
            LEFT JOIN streets s_from ON rs_from.street_id = s_from.id
            LEFT JOIN streets s_to ON rs_to.street_id = s_to.id
            LEFT JOIN user_info ui ON r.driver_id = ui.user_id
            WHERE r.status = 'ACTIVE'
              AND rs_from.stop_order < rs_to.stop_order
              AND rs_from.available_seats >= :seats
              AND r.departure_at >= :dayStart
              AND r.departure_at < :dayEnd
            """;

    private static final String FROM_DISTANCE_FILTER = """
              AND ST_DWithin(
                  COALESCE(s_from.geom, a_from.geom)::geography,
                  ST_SetSRID(ST_MakePoint(:fromLon, :fromLat), 4326)::geography,
                  :maxDistStart
              )
            """;

    private static final String TO_DISTANCE_FILTER = """
              AND ST_DWithin(
                  COALESCE(s_to.geom, a_to.geom)::geography,
                  ST_SetSRID(ST_MakePoint(:toLon, :toLat), 4326)::geography,
                  :maxDistEnd
              )
            """;

    private static final String MAX_PRICE_FILTER = """
              AND (rs_from.price_per_seat - rs_to.price_per_seat) <= :maxPrice
            """;

    private static final String ORDER_SQL = """
            ORDER BY dist_start_km ASC, r.departure_at ASC
            """;

    private final EntityManager entityManager;

    public RideSearchRepositoryImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Tuple> searchRides(final RideSearchRequestDTO request,
                                   final double fromLat, final double fromLon,
                                   final double toLat, final double toLon) {
        final StringBuilder sql = new StringBuilder(BASE_SEARCH_SQL);
        final Map<String, Object> params = new HashMap<>();

        addBaseParameters(params, request, fromLat, fromLon, toLat, toLon);
        addDistanceFilters(sql, params, request);
        addMaxPriceFilter(sql, params, request);
        sql.append(getTimeWindowFilter(request.timeWindow()));
        addDriverPreferenceFilters(sql, request);
        sql.append(ORDER_SQL);

        return executeSearch(sql, params);
    }

    private void addBaseParameters(final Map<String, Object> params,
                                   final RideSearchRequestDTO request,
                                   final double fromLat, final double fromLon,
                                   final double toLat, final double toLon) {
        params.put("fromLon", fromLon);
        params.put("fromLat", fromLat);
        params.put("toLon", toLon);
        params.put("toLat", toLat);
        params.put("seats", request.seats());
        params.put("dayStart", getSearchDayStart(request));
        params.put("dayEnd", getSearchDayEnd(request));
    }

    private OffsetDateTime getSearchDayStart(final RideSearchRequestDTO request) {
        return request.date().atStartOfDay(SEARCH_DAY_ZONE).toOffsetDateTime();
    }

    private OffsetDateTime getSearchDayEnd(final RideSearchRequestDTO request) {
        return request.date().plusDays(1L).atStartOfDay(SEARCH_DAY_ZONE).toOffsetDateTime();
    }

    private void addDistanceFilters(final StringBuilder sql,
                                    final Map<String, Object> params,
                                    final RideSearchRequestDTO request) {
        sql.append(FROM_DISTANCE_FILTER);
        params.put("maxDistStart", toMeters(getDistanceOrDefault(request.maxDistanceStart())));

        sql.append(TO_DISTANCE_FILTER);
        params.put("maxDistEnd", toMeters(getDistanceOrDefault(request.maxDistanceEnd())));
    }

    private double getDistanceOrDefault(final Double distanceKm) {
        if (distanceKm == null) {
            return DEFAULT_MAX_DISTANCE_KM;
        }
        return distanceKm;
    }

    private double toMeters(final double distanceKm) {
        return distanceKm * METERS_PER_KILOMETER;
    }

    private void addMaxPriceFilter(final StringBuilder sql,
                                   final Map<String, Object> params,
                                   final RideSearchRequestDTO request) {
        if (request.maxPrice() == null) {
            return;
        }

        sql.append(MAX_PRICE_FILTER);
        params.put("maxPrice", request.maxPrice());
    }

    private String getTimeWindowFilter(final String timeWindow) {
        if (timeWindow == null) {
            return "";
        }

        return switch (timeWindow) {
            case "BEFORE_8" -> "  AND EXTRACT(HOUR FROM r.departure_at) < 8\n";
            case "8_12" -> "  AND EXTRACT(HOUR FROM r.departure_at) BETWEEN 8 AND 11\n";
            case "12_18" -> "  AND EXTRACT(HOUR FROM r.departure_at) BETWEEN 12 AND 17\n";
            case "AFTER_18" -> "  AND EXTRACT(HOUR FROM r.departure_at) >= 18\n";
            default -> "";
        };
    }

    private void addDriverPreferenceFilters(final StringBuilder sql, final RideSearchRequestDTO request) {
        if (Boolean.TRUE.equals(request.petFriendly())) {
            sql.append("  AND ui.pet_friendly = true\n");
        }
        if (Boolean.TRUE.equals(request.smokingAllowed())) {
            sql.append("  AND ui.can_smoke = true\n");
        }
    }

    private List<Tuple> executeSearch(final StringBuilder sql, final Map<String, Object> params) {
        final Query query = entityManager.createNativeQuery(sql.toString(), Tuple.class);
        params.forEach(query::setParameter);
        final List<?> resultRows = query.getResultList();
        return resultRows.stream()
                .map(Tuple.class::cast)
                .toList();
    }
}
