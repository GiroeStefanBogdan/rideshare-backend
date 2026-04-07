package com.example.blablacar.repository.ride;

import com.example.blablacar.model.ride.Ride;
import com.example.blablacar.model.ride.RideStop;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
@Repository
public interface RideStopRepository extends JpaRepository<RideStop, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<RideStop> findAllByRide(Ride ride);

    void deleteAllByRide(Ride ride);
}
