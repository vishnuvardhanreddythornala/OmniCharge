package com.omnicharge.user.controller;
import static org.mockito.ArgumentMatchers.eq;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.user.common.exception.BadRequestException;
import com.omnicharge.user.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.AuthResponse;
import com.omnicharge.user.dto.SendMobileOtpRequest;
import com.omnicharge.user.dto.VerifyMobileOtpRequest;
import com.omnicharge.user.service.MobileVerificationService;
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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MobileVerificationController.class, excludeAutoConfiguration = {JpaRepositoriesAutoConfiguration.class, DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class MobileVerificationControllerTest {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private MobileVerificationService mobileVerificationService;

    @MockitoBean
    private LogEventPublisher logEventPublisher;


    @Autowired
    public MobileVerificationControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }
    @Test
    void sendOtp_Success() throws Exception {
        SendMobileOtpRequest req = new SendMobileOtpRequest();
        req.setMobileNumber("+919000000000");

        doNothing().when(mobileVerificationService).sendOtp(eq(1L), any(SendMobileOtpRequest.class));

        mockMvc.perform(post("/api/users/mobile-otp/send")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void verifyOtp_Success() throws Exception {
        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("+919000000000");
        req.setOtp("123456");

        AuthResponse resp = AuthResponse.builder().accessToken("token").build();

        when(mobileVerificationService.verifyOtp(eq(1L), any(VerifyMobileOtpRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/users/mobile-otp/verify")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("token"));
    }

    @Test
    void verifyOtp_InvalidOtp() throws Exception {
        VerifyMobileOtpRequest req = new VerifyMobileOtpRequest();
        req.setMobileNumber("+919000000000");
        req.setOtp("111111");

        when(mobileVerificationService.verifyOtp(eq(1L), any(VerifyMobileOtpRequest.class)))
                .thenThrow(new BadRequestException("Invalid OTP"));

        mockMvc.perform(post("/api/users/mobile-otp/verify")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

