package com.chss.service;

import com.chss.entity.User;
import com.chss.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** FR-5 / US-5: authenticate; returns empty on any failure (generic error upstream). */
    public Optional<User> authenticate(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPassword()));
    }

    public User createUser(String username, String rawPassword, com.chss.entity.Role role) {
        User u = new User(username, passwordEncoder.encode(rawPassword), role);
        return userRepository.save(u);
    }
}
