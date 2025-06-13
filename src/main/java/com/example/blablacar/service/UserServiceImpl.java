package com.example.blablacar.service;

import com.example.blablacar.dto.LoginRequest;
import com.example.blablacar.dto.UserRegistrationRequest;
import com.example.blablacar.enums.AuthProvider;
import com.example.blablacar.enums.Role;
import com.example.blablacar.exception.EmailAlreadyExistsException;
import com.example.blablacar.model.User;
import com.example.blablacar.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JWTService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    public User registerUser(UserRegistrationRequest userRegistrationRequest) {
        if (userRepository.existsByEmail(userRegistrationRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }
        userRegistrationRequest.setPassword(passwordEncoder.encode(userRegistrationRequest.getPassword()));
        User user = User.builder()
                .name(userRegistrationRequest.getName())
                .email(userRegistrationRequest.getEmail())
                .password(userRegistrationRequest.getPassword())
                .provider(AuthProvider.LOCAL)
                .providerId(null)
                .role(Role.ROLE_USER)
                .birthday(userRegistrationRequest.getBirthday())
                .phoneNumber(userRegistrationRequest.getPhoneNumber())
                .gender(userRegistrationRequest.getGender())
                .build();
        return userRepository.save(user);
    }

    @Override
    public String verify(LoginRequest user) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));

        if (authentication.isAuthenticated()) {
            return jwtService.generateToken(user.getEmail());
        }
        return null;
    }
}
