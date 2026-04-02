package com.omnicharge.common.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/*
 * Base Audit Configuration
 * Note: Each service must add @EnableJpaAuditing(auditorAwareRef = "auditorAware") 
 * to their own configuration class to enable auditing
 */
@Configuration
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            // Try to get authentication from SecurityContext if Spring Security is available
            try {
                Class<?> securityContextHolder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
                Object context = securityContextHolder.getMethod("getContext").invoke(null);
                Object authentication = context.getClass().getMethod("getAuthentication").invoke(context);
                
                if (authentication != null) {
                    Boolean isAuthenticated = (Boolean) authentication.getClass().getMethod("isAuthenticated").invoke(authentication);
                    Object principal = authentication.getClass().getMethod("getPrincipal").invoke(authentication);
                    
                    if (isAuthenticated && !"anonymousUser".equals(principal)) {
                        String name = (String) authentication.getClass().getMethod("getName").invoke(authentication);
                        return Optional.of(name);
                    }
                }
            } catch (Exception e) {
                // Spring Security not available or error occurred, use SYSTEM
            }
            return Optional.of("SYSTEM");
        };
    }
}
