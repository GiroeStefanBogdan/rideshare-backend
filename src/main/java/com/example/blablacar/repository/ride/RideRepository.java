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
              and exists (select finalStop.id from RideStop finalStop
                  where finalStop.ride = r
                    and finalStop.stopOrder = (select max(s.stopOrder) from RideStop s where s.ride = r)
                    and (finalStop.departsAt is null or finalStop.departsAt >= :from))
            order by r.departureAt asc
            """)
    List<Ride> findRecentByDriverId(final long driverId, final OffsetDateTime from);
}
