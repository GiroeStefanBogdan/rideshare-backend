package com.example.blablacar.service;

import com.example.blablacar.model.User;
import com.example.blablacar.model.UserPrincipal;
import com.example.blablacar.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
        @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
            System.out.println("🔍 Looking for user with email: " + email);

        Optional<User> user = userRepository.findByEmail(email);

        if(user.isEmpty()){
            throw new UsernameNotFoundException("User not found with email : " + email);
        }

        User authenticatedUser = user.get();
        return new UserPrincipal(authenticatedUser);
    }
}
