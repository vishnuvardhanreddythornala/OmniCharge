package com.omnicharge.operator.config;

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

@Component
@Slf4j
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_USER_EMAIL = "X-User-Email";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        String userRole = request.getHeader(HEADER_USER_ROLE);
        String userEmail = request.getHeader(HEADER_USER_EMAIL);

        if (userId != null && userRole != null) {
            // Create authentication token from gateway headers
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(userRole);
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userEmail, null, Collections.singletonList(authority));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("Authenticated user from gateway headers: userId={}, role={}, email={}", 
                userId, userRole, userEmail);
        }

        filterChain.doFilter(request, response);
    }
}
