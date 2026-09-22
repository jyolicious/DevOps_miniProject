package com.communityhealth.survey.config;

import com.communityhealth.survey.entity.User;
import com.communityhealth.survey.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            if (userRepository.findByUsername("surveyor").isEmpty()) {
                userRepository.save(
                    new User(
                        "surveyor",
                        passwordEncoder.encode("surveyor123"),
                        "SURVEYOR"
                    )
                );
            }

            if (userRepository.findByUsername("admin").isEmpty()) {
                userRepository.save(
                    new User(
                        "admin",
                        passwordEncoder.encode("admin123"),
                        "ADMIN"
                    )
                );
            }

            if (userRepository.findByUsername("healthofficer").isEmpty()) {
                userRepository.save(
                    new User(
                        "healthofficer",
                        passwordEncoder.encode("health123"),
                        "HEALTH_OFFICER"
                    )
                );
            }
        };
    }
}