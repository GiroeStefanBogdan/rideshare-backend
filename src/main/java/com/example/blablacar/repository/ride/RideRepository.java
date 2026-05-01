package com.example.blablacar.repository.ride;

import com.example.blablacar.model.ride.Ride;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}
