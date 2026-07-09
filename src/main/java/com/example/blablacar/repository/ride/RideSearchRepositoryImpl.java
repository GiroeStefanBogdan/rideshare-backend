package com.example.blablacar.repository.ride;

import com.example.blablacar.dto.ride.RideDriverDTO;
import com.example.blablacar.dto.ride.RideSearchRequestDTO;
import com.example.blablacar.dto.ride.RideSearchResultDTO;
import com.example.blablacar.dto.ride.RideStopBasicDTO;
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
                                                         u.id AS driver_id,
                                                         u.name AS driver_name,
                                                         ui.rating AS driver_rating,
                                                         ui.reviews_count AS driver_reviews,
                                                         ui.can_smoke AS driver_can_smoke,
                                                         ui.pet_friendly AS driver_pet_friendly,
                   rs_from.id AS rs_from_id,
                                                         rs_from.available_seats AS from_available_seats,
                                                         rs_from.price_per_seat AS from_price,
                                                         rs_from.departs_at AS from_departs_at,
                                                         COALESCE(s_from.name, a_from.name) AS from_location_name,
                   rs_to.id AS rs_to_id,
                                                         rs_to.price_per_seat AS to_price,
                                                         rs_to.departs_at AS to_departs_at,
                                                         COALESCE(s_to.name, a_to.name) AS to_location_name,
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
                                                  JOIN users u ON r.driver_id = u.id
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
    public List<RideSearchResultDTO> searchRides(final RideSearchRequestDTO request,
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

        final List<Tuple> rows = executeSearch(sql, params);
        return rows.stream().map(this::mapRow).toList();
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

    @SuppressWarnings("unchecked")
    private List<Tuple> executeSearch(final StringBuilder sql, final Map<String, Object> params) {
        final Query query = entityManager.createNativeQuery(sql.toString(), Tuple.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    private RideSearchResultDTO mapRow(final Tuple row) {
        long rideId = getLong(row, "ride_id");
        long driverId = getLong(row, "driver_id");
        String driverName = getString(row, "driver_name");
        Double driverRating = getDouble(row, "driver_rating");
        Integer driverReviews = getInt(row, "driver_reviews");
        boolean driverCanSmoke = getBoolean(row, "driver_can_smoke");
        boolean driverPetFriendly = getBoolean(row, "driver_pet_friendly");

        long rsFromId = getLong(row, "rs_from_id");
        Integer fromAvailableSeats = getInt(row, "from_available_seats");
        Number fromPrice = row.get("from_price", Number.class);
        OffsetDateTime fromDepartsAt = getOffsetDateTime(row, "from_departs_at");
        String fromLocationName = getString(row, "from_location_name");

        Number toPrice = row.get("to_price", Number.class);
        OffsetDateTime toDepartsAt = getOffsetDateTime(row, "to_departs_at");
        String toLocationName = getString(row, "to_location_name");

        double distStartKm = row.get("dist_start_km", Number.class).doubleValue();
        double distEndKm = row.get("dist_end_km", Number.class).doubleValue();

        double rating = driverRating != null ? driverRating : 0.0;
        int reviewsCount = driverReviews != null ? driverReviews : 0;

        RideDriverDTO driverDTO = new RideDriverDTO(
                driverId,
                driverName,
                null,
                rating,
                reviewsCount,
                driverCanSmoke,
                driverPetFriendly
        );

        RideStopBasicDTO startDTO = new RideStopBasicDTO(
                rsFromId,
                fromLocationName,
                fromDepartsAt != null ? fromDepartsAt.toString() : null
        );

        RideStopBasicDTO endDTO = new RideStopBasicDTO(
                getLong(row, "rs_to_id"),
                toLocationName,
                toDepartsAt != null ? toDepartsAt.toString() : null
        );

        int totalPrice = 0;
        if (fromPrice != null) {
            int endPrice = toPrice != null ? toPrice.intValue() : 0;
            totalPrice = fromPrice.intValue() - endPrice;
        }

        return new RideSearchResultDTO(
                rideId,
                driverDTO,
                fromAvailableSeats,
                totalPrice,
                startDTO,
                endDTO,
                distStartKm,
                distEndKm
        );
    }

    private long getLong(final Tuple row, final String alias) {
        return row.get(alias, Number.class).longValue();
    }

    private String getString(final Tuple row, final String alias) {
        return row.get(alias, String.class);
    }

    private Double getDouble(final Tuple row, final String alias) {
        Number value = row.get(alias, Number.class);
        return value != null ? value.doubleValue() : null;
    }

    private Integer getInt(final Tuple row, final String alias) {
        Number value = row.get(alias, Number.class);
        return value != null ? value.intValue() : null;
    }

    private boolean getBoolean(final Tuple row, final String alias) {
        Boolean value = row.get(alias, Boolean.class);
        return value != null && value;
    }

    private OffsetDateTime getOffsetDateTime(final Tuple row, final String alias) {
        return row.get(alias, OffsetDateTime.class);
    }
}
