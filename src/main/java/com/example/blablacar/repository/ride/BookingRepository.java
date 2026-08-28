package com.example.blablacar.repository.ride;

import com.example.blablacar.model.ride.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            select b from Booking b
            join fetch b.ride r
            join fetch r.driver d
            left join fetch d.userInfo
            join fetch b.fromStop fromStop
            join fetch fromStop.administrativeUnit
            left join fetch fromStop.street
            join fetch b.toStop toStop
            join fetch toStop.administrativeUnit
            left join fetch toStop.street
            where b.passenger.id = :passengerId
              and fromStop.departsAt >= :from
            order by fromStop.departsAt asc
            """)
    List<Booking> findUpcomingByPassengerId(final long passengerId, final OffsetDateTime from);

    @Query("""
            select b from Booking b
            join fetch b.ride r
            join fetch r.driver d
            left join fetch d.userInfo
            join fetch b.fromStop fromStop
            join fetch fromStop.administrativeUnit
            left join fetch fromStop.street
            join fetch b.toStop toStop
            join fetch toStop.administrativeUnit
            left join fetch toStop.street
            where b.passenger.id = :passengerId
              and fromStop.departsAt >= :from
              and fromStop.departsAt < :to
            order by fromStop.departsAt desc
            """)
    List<Booking> findPastByPassengerId(final long passengerId, final OffsetDateTime from,
                                        final OffsetDateTime to);
}
