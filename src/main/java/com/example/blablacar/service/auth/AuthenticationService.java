package com.example.blablacar.service.auth;

import com.example.blablacar.dto.auth.LoginRequest;
import com.example.blablacar.dto.auth.LoginResponse;
import com.example.blablacar.dto.user.UserResponseDto;
import com.example.blablacar.exception.user.UserNotFoundException;
import com.example.blablacar.model.user.User;
import com.example.blablacar.repository.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final UserRepository userRepository;

    public AuthenticationService(final AuthenticationManager authenticationManager,
                                 final JWTService jwtService,
                                 final UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public LoginResponse verify(final LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        if (authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            if (userDetails == null) {
                throw new BadCredentialsException("Authentication doesn't have correct principal");
            }
            String token = jwtService.generateToken(userDetails, loginRequest.getRememberMe());

            User foundUser = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(
                            () -> new UserNotFoundException("User not found with email " + loginRequest.getEmail()));

            return new LoginResponse(token, UserResponseDto.from(foundUser));
        }
        throw new BadCredentialsException("Invalid username or password");
    }
}
