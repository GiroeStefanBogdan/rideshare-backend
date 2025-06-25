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
        if (userRepository.existsByEmail(userRegistrationRequest.email())) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }
        User user = User.builder()
                .name(userRegistrationRequest.name())
                .email(userRegistrationRequest.email())
                .password(userRegistrationRequest.password())
                .provider(AuthProvider.LOCAL)
                .providerId(null)
                .role(Role.ROLE_USER)
                .birthday(userRegistrationRequest.birthday())
                .phoneNumber(userRegistrationRequest.phoneNumber())
                .gender(userRegistrationRequest.gender())
                .password(passwordEncoder.encode(userRegistrationRequest.password()))
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
