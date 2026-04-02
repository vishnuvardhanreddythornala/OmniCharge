package com.omnicharge.recharge.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Slf4j
public class FeignClientInterceptor implements RequestInterceptor {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_USER_EMAIL = "X-User-Email";

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            // Forward authentication headers from Gateway to downstream services
            String userId = request.getHeader(HEADER_USER_ID);
            String userRole = request.getHeader(HEADER_USER_ROLE);
            String userEmail = request.getHeader(HEADER_USER_EMAIL);
            
            if (userId != null) {
                template.header(HEADER_USER_ID, userId);
                log.debug("Forwarding X-User-Id: {}", userId);
            }
            
            if (userRole != null) {
                template.header(HEADER_USER_ROLE, userRole);
                log.debug("Forwarding X-User-Role: {}", userRole);
            }
            
            if (userEmail != null) {
                template.header(HEADER_USER_EMAIL, userEmail);
                log.debug("Forwarding X-User-Email: {}", userEmail);
            }
        }
    }
}
