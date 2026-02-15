package com.example.blablacar.service;

import com.example.blablacar.dto.*;
import com.example.blablacar.model.enums.Role;
import com.example.blablacar.model.user.User;

import java.util.List;


public interface UserService {

    UserResponseDto registerUser(UserRegistrationRequestDto registerRequest);

    LoginResponse verify(LoginRequest user);

    List<UserResponseDto> getAllUsers();

    Object getUserById(long id, User authenticatedUser);

    UserResponseDto updateUserById(long requestedUserId, UpdateUserRequestDto updateUserRequestDto);

    void changeUserPassword(String email, LoginRequest loginRequest);

    UserResponseDto updateUserRole(long id, Role role);

    void deleteUserById(Long id);

    void deleteMyAccount(Long id);
}
