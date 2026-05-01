package com.example.blablacar.repository.ride;

import com.example.blablacar.dto.ride.RideSearchRequestDTO;
import jakarta.persistence.Tuple;

import java.util.List;

/**
 * Custom repository for the ride search native query.
 */
public interface RideSearchRepository {

    /**
     * Searches for rides matching the given criteria.
     * Each returned {@code Tuple} contains:
     * <ol>
     *   <li>"ride_id" (Number)</li>
     *   <li>"rs_from_id" (Number)</li>
     *   <li>"rs_to_id" (Number)</li>
     *   <li>"dist_start_km" (Number)</li>
     *   <li>"dist_end_km" (Number)</li>
     * </ol>
     */
    List<Tuple> searchRides(RideSearchRequestDTO request,
                            double fromLat, double fromLon,
                            double toLat, double toLon);
}
