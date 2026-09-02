package com.communityhealth.survey.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                // Public pages
                .requestMatchers("/", "/login", "/css/**", "/js/**")
                .permitAll()

                // Dashboard
                .requestMatchers("/dashboard")
                .hasAnyRole("ADMIN", "HEALTH_OFFICER")

                // Status changes only by admin
                .requestMatchers("/surveys/*/status")
                .hasRole("ADMIN")

                // Creating surveys
                .requestMatchers("/surveys/create")
                .hasRole("SURVEYOR")

                // Updating surveys
                .requestMatchers("/surveys/*/edit", "/surveys/*/update")
                .hasAnyRole("SURVEYOR", "ADMIN")

                // All other survey operations require login
                .requestMatchers("/surveys/**")
                .authenticated()

                // Everything else
                .anyRequest()
                .authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/surveys", true)
                .failureUrl("/login?error")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}