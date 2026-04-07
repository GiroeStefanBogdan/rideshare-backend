package com.example.blablacar.repository.user.car;

import com.example.blablacar.model.user.UserCar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Author: Giroe Stefan Bogdan
 * Since: 29.03.2026
 */
@Repository
public interface UserCarRepository extends JpaRepository<UserCar, Long> {

    List<UserCar> findAllByUser_Id(long id);
    void deleteByIdAndUser_Id(long id, long userId);
    Optional<UserCar> findByIdAndUser_Id(long carId, long userId);
}
