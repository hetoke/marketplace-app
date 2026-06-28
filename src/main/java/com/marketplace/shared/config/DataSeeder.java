package com.marketplace.shared.config;

import com.marketplace.user.model.User;
import com.marketplace.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdminAccount();
    }

    private void seedAdminAccount() {
        String email = System.getenv().getOrDefault("ADMIN_EMAIL", "admin@marketplace.com");
        String password = System.getenv().getOrDefault("ADMIN_PASSWORD", "Admin123!");

        if (userRepository.existsByEmail(email)) {
            log.info("Admin account already exists: {}", email);
            return;
        }

        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        admin.setVerified(true);
        admin.setAuthenticationType(User.AuthenticationType.LOCAL);
        admin.setDisplayName("Admin");
        userRepository.save(admin);

        log.info("Admin account created: {}", email);
    }
}
