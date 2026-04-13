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

    @Test
    void googleAuth_Success() throws Exception {
        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setIdToken("google_id_token_123");

        when(authService.authenticateWithGoogle(any(GoogleAuthRequest.class), anyString()))
                .thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"));
    }

    @Test
    void refreshToken_Success() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("old_refresh_token");

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"));
    }

    @Test
    void sendEmailLoginOtp_Success() throws Exception {
        SendEmailOtpRequest req = new SendEmailOtpRequest();
        req.setEmail("user@example.com");

        doNothing().when(authService).sendEmailOtp(any(SendEmailOtpRequest.class), anyString());

        mockMvc.perform(post("/api/auth/email/send-login-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void verifyEmailLoginOtp_Success() throws Exception {
        VerifyEmailOtpRequest req = new VerifyEmailOtpRequest();
        req.setEmail("user@example.com");
        req.setOtp("123456");

        when(authService.verifyEmailOtp(any(VerifyEmailOtpRequest.class)))
                .thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/auth/email/verify-login-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"));
    }

    @Test
    void verifyAdmin2fa_Success() throws Exception {
        VerifyEmailOtpRequest req = new VerifyEmailOtpRequest();
        req.setEmail("admin@omnicharge.com");
        req.setOtp("123456");

        when(authService.verifyAdmin2fa(any(VerifyEmailOtpRequest.class), anyString()))
                .thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/auth/admin/verify-2fa")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"));
    }

    @Test
    void logout_Success() throws Exception {
        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer mock_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void logout_WithRefreshToken() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("refresh_token_123");

        doNothing().when(authService).logout(anyString());
        doNothing().when(authService).logoutByRefreshToken(anyString());

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer mock_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void sendEmailVerification_Success() throws Exception {
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.get("userId", String.class)).thenReturn("1");
        when(jwtUtil.validateToken(anyString())).thenReturn(claims);
        doNothing().when(emailVerificationService).sendVerificationOtp(anyLong(), anyString());

        mockMvc.perform(post("/api/auth/email/send-verification")
                .header("Authorization", "Bearer mock_token")
                .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void verifyEmail_Success() throws Exception {
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.get("userId", String.class)).thenReturn("1");
        when(jwtUtil.validateToken(anyString())).thenReturn(claims);
        doNothing().when(emailVerificationService).verifyEmail(anyLong(), anyString());

        mockMvc.perform(post("/api/auth/email/verify")
                .header("Authorization", "Bearer mock_token")
                .param("otp", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void login_WithForwardedIp() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@omnicharge.com");
        req.setPassword("pass");

        AdminLoginInitResponse initResponse = AdminLoginInitResponse.builder()
                .requires2fa(true).email("admin@omnicharge.com").build();
        when(authService.login(any(LoginRequest.class), anyString())).thenReturn(initResponse);

        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "192.168.1.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
