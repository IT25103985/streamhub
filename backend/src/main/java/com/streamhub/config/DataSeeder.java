package com.streamhub.config;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
//startup seeder creating initial admin and moderator accounts and ensuring unicode columns
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${seed.admin.password}")
    private String adminPassword;

    @Value("${seed.moderator.password}")
    private String moderatorPassword;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        //ensure database columns are NVARCHAR for unicode, sinhala, and stylized character support
        migrateColumnsToUnicode();

        //seed admin account if it does not exist
        if (!userRepository.existsByUsernameIgnoreCase("admin")) {
            User admin = new User(
                    null,
                    "admin",
                    "admin@streamhub.com",
                    "Admin",
                    "User",
                    java.time.LocalDate.of(2000, 1, 1),
                    passwordEncoder.encode(adminPassword),
                    "ADMINISTRATOR",
                    "ACTIVE",
                    null,
                    null
            );
            userRepository.save(admin);
            System.out.println("Seeded ADMINISTRATOR account with username 'admin'");
        }

        //seed moderator account if it does not exist
        if (!userRepository.existsByUsernameIgnoreCase("moderator")) {
            User moderator = new User(
                    null,
                    "moderator",
                    "moderator@streamhub.com",
                    "Moderator",
                    "User",
                    java.time.LocalDate.of(2000, 1, 1),
                    passwordEncoder.encode(moderatorPassword),
                    "CONTENT_MODERATOR",
                    "ACTIVE",
                    null,
                    null
            );
            userRepository.save(moderator);
            System.out.println("Seeded CONTENT_MODERATOR account with username 'moderator'");
        }
    }

    private void migrateColumnsToUnicode() {
        String[] alterStatements = new String[] {
            "ALTER TABLE Video ALTER COLUMN title NVARCHAR(255) NOT NULL",
            "ALTER TABLE Video ALTER COLUMN description NVARCHAR(MAX)",
            "ALTER TABLE Playlist ALTER COLUMN name NVARCHAR(255) NOT NULL",
            "ALTER TABLE Playlist ALTER COLUMN description NVARCHAR(1000)",
            "ALTER TABLE categories ALTER COLUMN description NVARCHAR(500)",
            "ALTER TABLE [User] ALTER COLUMN first_name NVARCHAR(100) NOT NULL",
            "ALTER TABLE [User] ALTER COLUMN last_name NVARCHAR(100) NOT NULL",
            "ALTER TABLE [User] ALTER COLUMN bio NVARCHAR(1000)"
        };

        for (String sql : alterStatements) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                //column already altered or constraint in place
            }
        }
    }
}
