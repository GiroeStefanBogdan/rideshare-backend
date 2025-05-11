package com.example.blablacar.service;

import com.example.blablacar.dto.LoginRequest;
import com.example.blablacar.dto.RegisterRequest;
import com.example.blablacar.model.User;
import org.springframework.stereotype.Service;


public interface UserService {

    User registerUser(User user);

    String verify(LoginRequest user);
}
