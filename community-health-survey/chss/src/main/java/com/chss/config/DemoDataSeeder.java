package com.chss.config;

import com.chss.entity.Role;
import com.chss.repository.UserRepository;
import com.chss.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds one demo user per role so all three roles can be demonstrated locally
 * without a separate SQL import step. Passwords are for local MVP demo only -
 * see Week 1 constraint: use dummy/anonymized data only, not production credentials.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    public DemoDataSeeder(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userService.createUser("surveyor1", "password123", Role.SURVEYOR);
            userService.createUser("admin1", "password123", Role.ADMINISTRATOR);
            userService.createUser("officer1", "password123", Role.HEALTH_OFFICER);
            System.out.println("Seeded demo users: surveyor1 / admin1 / officer1 (password123)");
        }
    }
}
