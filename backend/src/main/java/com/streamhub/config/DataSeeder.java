package com.streamhub.config;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.admin.password}")
    private String adminPassword;

    @Value("${seed.moderator.password}")
    private String moderatorPassword;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed Administrator
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User(
                    null,
                    "admin",
                    "admin@streamhub.com",
                    passwordEncoder.encode(adminPassword),
                    "ADMINISTRATOR",
                    null,
                    null
            );
            userRepository.save(admin);
            System.out.println("Seeded ADMINISTRATOR account with username 'admin'");
        }

        // Seed Content Moderator
        if (!userRepository.existsByUsername("moderator")) {
            User moderator = new User(
                    null,
                    "moderator",
                    "moderator@streamhub.com",
                    passwordEncoder.encode(moderatorPassword),
                    "CONTENT_MODERATOR",
                    null,
                    null
            );
            userRepository.save(moderator);
            System.out.println("Seeded CONTENT_MODERATOR account with username 'moderator'");
        }
    }
}
