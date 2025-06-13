package com.example.blablacar.service;

import com.example.blablacar.dto.LoginRequest;
import com.example.blablacar.dto.UserRegistrationRequest;
import com.example.blablacar.model.User;


public interface UserService {

    User registerUser(UserRegistrationRequest registerRequest);

    String verify(LoginRequest user);
}
