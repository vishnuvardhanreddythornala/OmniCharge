package com.omnicharge.user.config;

import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.Role;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.demo-password}")
    private String demoPassword;

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedDemoUser();
    }

    private void seedAdminUser() {
        String adminEmail = "vishnuvardhanreddythornala@gmail.com";
        
        if (userRepository.existsByEmail(adminEmail)) {
            // Always update password to ensure correct bcrypt hash (Flyway seed uses a dummy hash)
            userRepository.findByEmail(adminEmail).ifPresent(admin -> {
                admin.setPassword(passwordEncoder.encode(adminPassword));
                userRepository.save(admin);
                log.info("Admin user password updated: {}", adminEmail);
            });
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setFullName("Admin User");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setMobileNumber("+919999999999");
        admin.setAuthProvider(AuthProvider.LOCAL);
        admin.setRole(Role.ROLE_ADMIN);
        admin.setIsActive(true);
        admin.setIsMobileVerified(true);

        try {
            userRepository.save(admin);
            log.info("Admin user created: {}", adminEmail);
        } catch (Exception e) {
            log.warn("Failed to create admin user (possibly duplicate mobile constraint): {}", e.getMessage());
        }
    }

    private void seedDemoUser() {
        String demoEmail = "user1@omnicharge.com";
        
        if (userRepository.existsByEmail(demoEmail)) {
            log.info("Demo user already exists");
            return;
        }

        User demoUser = new User();
        demoUser.setEmail(demoEmail);
        demoUser.setFullName("Demo User");
        demoUser.setPassword(passwordEncoder.encode(demoPassword));
        demoUser.setMobileNumber("+919876543210");
        demoUser.setAuthProvider(AuthProvider.LOCAL);
        demoUser.setRole(Role.ROLE_USER);
        demoUser.setIsActive(true);
        demoUser.setIsMobileVerified(true);

        try {
            userRepository.save(demoUser);
            log.info("Demo user created: {}", demoEmail);
        } catch (Exception e) {
            log.warn("Failed to create demo user (possibly duplicate mobile constraint): {}", e.getMessage());
        }
    }
}
