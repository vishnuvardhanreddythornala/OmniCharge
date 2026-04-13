package com.omnicharge.user.controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.user.common.exception.BadRequestException;
import com.omnicharge.user.common.exception.ForbiddenException;
import com.omnicharge.user.common.exception.UnauthorizedException;
import com.omnicharge.user.dto.*;
import com.omnicharge.user.service.EmailVerificationService;
import com.omnicharge.user.service.IAuthService;
import com.omnicharge.user.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {JpaRepositoriesAutoConfiguration.class, DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false) // Disables Spring Security filters for pure unit testing
class AuthControllerTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @MockitoBean(name="logEventPublisher")
    private com.omnicharge.user.common.logging.LogEventPublisher logEventPublisher;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IAuthService authService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private AuthResponse mockAuthResponse;

    @BeforeEach
    void setUp() {
        mockAuthResponse = AuthResponse.builder()
                .accessToken("mock_access_token")
                .refreshToken("mock_refresh_token")
                .build();
    }

    @Test
    void login_Success() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@omnicharge.com");
        req.setPassword("pass");

        AdminLoginInitResponse initResponse = AdminLoginInitResponse.builder()
                .requires2fa(true).email("admin@omnicharge.com").build();

        when(authService.login(any(LoginRequest.class), anyString())).thenReturn(initResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requires2fa").value(true));
    }

    @Test
    void login_Unauthorized() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@omnicharge.com");
        req.setPassword("wrongpass");

        when(authService.login(any(LoginRequest.class), anyString()))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void sendMobileOtp_Success() throws Exception {
        SendMobileOtpRequest req = new SendMobileOtpRequest();
        req.setMobileNumber("+919000000000");

        doNothing().when(authService).sendMobileOtp(any(SendMobileOtpRequest.class), anyString());

        mockMvc.perform(post("/api/auth/mobile/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void sendMobileOtp_ValidationError() throws Exception {
        SendMobileOtpRequest req = new SendMobileOtpRequest();
        // Empty mobile number

        mockMvc.perform(post("/api/auth/mobile/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyMobileOtp_Success() throws Exception {
        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("+919000000000");
        req.setOtp("123456");

        when(authService.verifyMobileOtp(any(VerifyMobileOtpRequest.class))).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/auth/mobile/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"));
    }

    @Test
    void verifyMobileOtp_InvalidOtp() throws Exception {
        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("+919000000000");
        req.setOtp("111111");

        when(authService.verifyMobileOtp(any(VerifyMobileOtpRequest.class)))
                .thenThrow(new BadRequestException("Invalid or expired OTP"));

        mockMvc.perform(post("/api/auth/mobile/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP"));
    }

    @Test
    void verifyMobileOtp_AdminForbidden() throws Exception {
        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("+919000000000");
        req.setOtp("123456");

        when(authService.verifyMobileOtp(any(VerifyMobileOtpRequest.class)))
                .thenThrow(new ForbiddenException("Administrators must use the secure Admin Portal"));

        mockMvc.perform(post("/api/auth/mobile/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}


