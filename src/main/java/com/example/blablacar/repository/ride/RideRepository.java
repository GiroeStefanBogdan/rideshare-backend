package com.example.blablacar.repository.ride;

import com.example.blablacar.model.ride.Ride;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
@Repository
@Transactional
public interface RideRepository extends JpaRepository<Ride, Long>, RideSearchRepository {

    @Query("select r from Ride r where r.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Ride> findByIdForUpdate(long id);

    @Query("""
            select distinct r from Ride r
            join fetch r.driver d
            left join fetch d.userInfo
            join fetch r.rideStops stop
            join fetch stop.administrativeUnit
            left join fetch stop.street
            where r.driver.id = :driverId
              and r.departureAt >= :from
            order by r.departureAt asc
            """)
    List<Ride> findUpcomingByDriverId(final long driverId, final OffsetDateTime from);

    @Query("""
            select distinct r from Ride r
            join fetch r.driver d
            left join fetch d.userInfo
            join fetch r.rideStops stop
            join fetch stop.administrativeUnit
            left join fetch stop.street
            where r.driver.id = :driverId
              and r.departureAt >= :from
              and r.departureAt < :to
            order by r.departureAt desc
            """)
    List<Ride> findPastByDriverId(final long driverId, final OffsetDateTime from,
                                  final OffsetDateTime to);
}
