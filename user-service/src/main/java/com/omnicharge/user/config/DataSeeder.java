package com.omnicharge.user.config;

import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.Role;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedDemoUser();
    }

    private void seedAdminUser() {
        String adminEmail = "admin@omnicharge.com";
        
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists");
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setFullName("Admin User");
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setMobileNumber("9999999999");
        admin.setAuthProvider(AuthProvider.LOCAL);
        admin.setRole(Role.ROLE_ADMIN);
        admin.setIsActive(true);

        userRepository.save(admin);
        log.info("Admin user created: {}", adminEmail);
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
        demoUser.setPassword(passwordEncoder.encode("User@123"));
        demoUser.setMobileNumber("9876543210");
        demoUser.setAuthProvider(AuthProvider.LOCAL);
        demoUser.setRole(Role.ROLE_USER);
        demoUser.setIsActive(true);

        userRepository.save(demoUser);
        log.info("Demo user created: {}", demoEmail);
    }
}
