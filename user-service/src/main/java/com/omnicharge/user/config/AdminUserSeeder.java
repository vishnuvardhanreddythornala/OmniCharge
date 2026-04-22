package com.omnicharge.user.config;

import com.omnicharge.user.entity.User;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.Role;import com.omnicharge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password:Login@630}")
    private String adminPassword;

    private static final String ADMIN_EMAIL = "vishnuvardhanreddythornala@gmail.com";

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking Admin user state...");

        Optional<User> adminOpt = userRepository.findByEmail(ADMIN_EMAIL);

        if (adminOpt.isEmpty()) {
            log.info("Admin user not found. Creating new Admin user...");
            User newAdmin = new User();
            newAdmin.setEmail(ADMIN_EMAIL);
            newAdmin.setFullName("OmniCharge Admin");
            newAdmin.setMobileNumber("+919999900000");
            newAdmin.setPassword(passwordEncoder.encode(adminPassword));
            newAdmin.setAuthProvider(AuthProvider.LOCAL);
            newAdmin.setRole(Role.ROLE_ADMIN);
            newAdmin.setIsActive(true);
            newAdmin.setIsMobileVerified(true);
            newAdmin.setIsEmailVerified(true);
            
            userRepository.save(newAdmin);
            log.info("Admin user created successfully.");
        } else {
            User existingAdmin = adminOpt.get();
            if (!passwordEncoder.matches(adminPassword, existingAdmin.getPassword())) {
                log.warn("Admin password hash mismatch detected! Syncing password with application configuration...");
                existingAdmin.setPassword(passwordEncoder.encode(adminPassword));
                userRepository.save(existingAdmin);
                log.info("Admin password synced successfully.");
            } else {
                log.info("Admin user state is valid and up to date.");
            }
        }
    }
}
