package com.omnicharge.user.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter to extract user authentication from gateway headers.
 * The API Gateway validates JWT and adds X-User-Id, X-User-Role, X-User-Email headers.
 * This filter reads those headers and creates Spring Security authentication context.
 */
@Component
@Slf4j
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_USER_EMAIL = "X-User-Email";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        String userRole = request.getHeader(HEADER_USER_ROLE);
        String userEmail = request.getHeader(HEADER_USER_EMAIL);

        // If headers are present, create authentication object
        if (userId != null && userRole != null && userEmail != null) {
            log.debug("Authenticating user from gateway headers: userId={}, role={}, email={}", 
                     userId, userRole, userEmail);

            // Create authentication token with user details
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(userRole))
                    );

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("Authentication set successfully for user: {}", userId);
        } else {
            log.debug("No gateway headers found, skipping authentication");
        }

        filterChain.doFilter(request, response);
    }
}
