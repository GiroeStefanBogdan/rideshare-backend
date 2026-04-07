package com.example.blablacar.service.user.car;

import com.example.blablacar.dto.user.car.UpdateUserCarRequest;
import com.example.blablacar.dto.user.car.UserCarRequest;
import com.example.blablacar.dto.user.car.UserCarResponse;
import com.example.blablacar.exception.user.UserCarNotFoundException;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserCar;
import com.example.blablacar.repository.user.car.UserCarRepository;
import com.example.blablacar.service.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

/**
 * Author: Giroe Stefan Bogdan
 * Since: 29.03.2026
 */
@Service
public class UserCarService {

    private final UserService userService;
    private final UserCarRepository userCarRepository;
    private final JsonMapper mapper;

    public UserCarService(UserService userService, UserCarRepository userCarRepository, JsonMapper mapper) {
        this.userService = userService;
        this.userCarRepository = userCarRepository;
        this.mapper = mapper;
    }

    @Transactional
    public UserCarResponse createUserCar(UserCarRequest request, long userId) {
        User user = userService.getReferenceById(userId);
        UserCar userCar = new UserCar(user, request.brand(), request.model(), request.color(), request.year(), request.licensePlate(), request.numberOfSeats());
        UserCar savedCar = userCarRepository.save(userCar);
        return toDto(savedCar);
    }

    @Transactional
    public void deleteUserCar(long userId, long carId) {
        userCarRepository.deleteByIdAndUser_Id(carId, userId);
    }

    @Transactional(readOnly = true)
    public List<UserCarResponse> getUserCarsByUserId(long userId) {
        return userCarRepository.findAllByUser_Id(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public UserCarResponse updateUserCar(UpdateUserCarRequest request, long userId, long carId) {
        Optional<UserCar> userCar = userCarRepository.findByIdAndUser_Id(carId, userId);
        if (userCar.isEmpty()) {
            throw new UserCarNotFoundException(carId);
        }

         UserCar userCarEntity = userCar.get();

        if (request.brand() != null) {
            userCarEntity.setBrand(request.brand());
        }
        if (request.model() != null) {
            userCarEntity.setModel(request.model());
        }
        if (request.color() != null) {
            userCarEntity.setColor(request.color());
        }
        if (request.year() != null) {
            userCarEntity.setYear(request.year());
        }
        if (request.licensePlate() != null) {
            userCarEntity.setLicensePlate(request.licensePlate());
        }
        if (request.numberOfSeats() != null) {
            userCarEntity.setNumberOfSeats(request.numberOfSeats());
        }

        return toDto(userCarEntity);
    }

    private UserCarResponse toDto(UserCar userCar) {
        UserCarRequest details = mapper.convertValue(userCar, UserCarRequest.class);

        return new UserCarResponse(
                userCar.getId(),
                userCar.getUserId(),
                details
        );
    }
}
