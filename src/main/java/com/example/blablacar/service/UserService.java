package com.example.blablacar.service;

import com.example.blablacar.dto.LoginRequest;
import com.example.blablacar.model.user.User;


public interface UserService {

    User registerUser(User user);

    String verify(LoginRequest user);
}
