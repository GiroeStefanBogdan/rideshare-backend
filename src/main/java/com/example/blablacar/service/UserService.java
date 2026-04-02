package com.example.blablacar.service;

import com.example.blablacar.dto.LoginRequest;
import com.example.blablacar.dto.LoginResponse;
import com.example.blablacar.dto.UpdateUserRequestDto;
import com.example.blablacar.dto.UserProfileDto;
import com.example.blablacar.dto.UserRegistrationRequestDto;
import com.example.blablacar.dto.UserResponseDto;
import com.example.blablacar.model.enums.Role;
import com.example.blablacar.model.user.User;

import java.util.List;


public interface UserService {

    UserResponseDto registerUser(UserRegistrationRequestDto registerRequest);

    LoginResponse verify(LoginRequest user);

    List<UserResponseDto> getAllUsers();

    UserProfileDto getUserById(User authenticatedUser);

    User findById(long id);

    UserResponseDto updateUserById(long requestedUserId, UpdateUserRequestDto updateUserRequestDto);

    void changeUserPassword(String email, LoginRequest loginRequest);

    UserResponseDto updateUserRole(long id, Role role);

    void deleteUserById(Long id);

    void deleteMyAccount(Long id);

    User getReferenceById(long id);
}
