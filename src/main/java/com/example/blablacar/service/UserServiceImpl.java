package com.example.blablacar.service;

import com.example.blablacar.dto.LoginRequest;
import com.example.blablacar.dto.LoginResponse;
import com.example.blablacar.dto.UpdateUserRequestDto;
import com.example.blablacar.dto.UserPublicProfileDto;
import com.example.blablacar.dto.UserRegistrationRequestDto;
import com.example.blablacar.dto.UserResponseDto;
import com.example.blablacar.exception.user.EmailAlreadyExistsException;
import com.example.blablacar.exception.user.InvalidAgeException;
import com.example.blablacar.exception.user.UserNotFoundException;
import com.example.blablacar.model.enums.Role;
import com.example.blablacar.model.user.User;
import com.example.blablacar.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final JsonMapper mapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JWTService jwtService) {
        this.mapper = new JsonMapper();
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    public UserResponseDto registerUser(UserRegistrationRequestDto userRegistrationRequest) {
        if (userRepository.existsByEmail(userRegistrationRequest.email())) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        User user = new User(userRegistrationRequest.name(), userRegistrationRequest.email(),
                passwordEncoder.encode(userRegistrationRequest.password()), userRegistrationRequest.gender(),
                userRegistrationRequest.birthday(), userRegistrationRequest.phoneNumber());
        return mapper.convertValue(userRepository.save(user), UserResponseDto.class);
    }

    @Override
    public LoginResponse verify(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        if (authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            if (userDetails == null) {
                throw new BadCredentialsException("Authentication doesn't have correct principal");
            }
            String token = jwtService.generateToken(userDetails);

            User foundUser = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(
                            () -> new UserNotFoundException("User not found with email " + loginRequest.getEmail()));

            return new LoginResponse(token, UserResponseDto.from(foundUser));
        }
        throw new BadCredentialsException("Invalid username or password");
    }

    @Override
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> mapper.convertValue(user, UserResponseDto.class))
                .toList();
    }

    @Override
    public Object getUserById(long requestedUserId, User authenticatedUser) {
        long authenticatedUserId = authenticatedUser.getId();
        User user =
                userRepository.findById(requestedUserId).orElseThrow(() -> new UserNotFoundException(requestedUserId));

        // if viewing own profile or the authenticated user is admin than return full details
        if (user.getId() == authenticatedUserId || authenticatedUser.getRole().equals(Role.ROLE_ADMIN)) {
            return getFullProfile(user);
        }

        // if viewing others, return public profile
        return getPublicProfile(user);

    }

    @Override
    public UserResponseDto updateUserById(long id, UpdateUserRequestDto updateUserRequestDto) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        // if the new email is different then the existing email
        if (updateUserRequestDto.email() != null && !updateUserRequestDto.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(updateUserRequestDto.email())) {
                throw new EmailAlreadyExistsException("This email already exist");
            }

            user.setEmail(updateUserRequestDto.email());
        }

        if (updateUserRequestDto.name() != null && !updateUserRequestDto.name().isBlank()) {
            user.setName(updateUserRequestDto.name());
        }

        if (updateUserRequestDto.phoneNumber() != null && !updateUserRequestDto.phoneNumber().isBlank()) {
            user.setPhoneNumber(updateUserRequestDto.phoneNumber());
        }

        if (updateUserRequestDto.birthday() != null) {
            validateAge(updateUserRequestDto.birthday());
            user.setBirthday(updateUserRequestDto.birthday());
        }

        if (updateUserRequestDto.gender() != null) {
            user.setGender(updateUserRequestDto.gender());
        }

        return mapper.convertValue(userRepository.save(user), UserResponseDto.class);

    }

    @Override
    public void changeUserPassword(String email, LoginRequest loginRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email " + email));
        user.setPassword(passwordEncoder.encode(loginRequest.getPassword()));

        userRepository.save(user);
    }

    @Override
    public UserResponseDto updateUserRole(long id, Role newRole) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        user.setRole(newRole);

        return mapper.convertValue(userRepository.save(user), UserResponseDto.class);
    }

    @Override
    public void deleteUserById(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
        }
    }

    @Override
    public void deleteMyAccount(Long id) {
        deleteUserById(id);
    }

    private void validateAge(LocalDate birthday) {
        if (birthday == null) {
            throw new InvalidAgeException("Birthday cannot be null");
        }

        int age = Period.between(birthday, LocalDate.now()).getYears();

        if (age < 18) {
            throw new InvalidAgeException("User must be at least 18 years old");
        }
    }

    private UserResponseDto getFullProfile(User user) {
        return UserResponseDto.from(user);
    }

    private UserPublicProfileDto getPublicProfile(User user) {
        return UserPublicProfileDto.from(user);
    }
}
