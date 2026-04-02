package com.omnicharge.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.user.dto.ChangePasswordRequest;
import com.omnicharge.user.dto.UpdateProfileRequest;
import com.omnicharge.user.dto.UserProfileResponse;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.Role;
import com.omnicharge.user.service.IUserService;
import com.omnicharge.common.logging.LogEventPublisher;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false) // Disables Spring Security filters for isolation
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IUserService userService;

    @MockBean
    private LogEventPublisher logEventPublisher;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    private UserProfileResponse profileResponse;

    @BeforeEach
    void setUp() {
        profileResponse = UserProfileResponse.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .mobileNumber("9876543210")
                .role(Role.ROLE_USER)
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .build();
    }

    @Test
    void getProfile_Success() throws Exception {
        when(userService.getProfile(1L)).thenReturn(profileResponse);

        mockMvc.perform(get("/api/users/profile")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Test User"));
    }

    @Test
    void getProfile_MissingHeader() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError()); // Custom ExceptionHandler maps MissingRequestHeaderException to 500
    }

    @Test
    void updateProfile_Success() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setMobileNumber("9999999999");

        UserProfileResponse updatedResponse = UserProfileResponse.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Updated Name")
                .mobileNumber("9999999999")
                .role(Role.ROLE_USER)
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .build();

        when(userService.updateProfile(anyLong(), any(UpdateProfileRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/users/profile")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.data.mobileNumber").value("9999999999"));
    }

    @Test
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");

        doNothing().when(userService).changePassword(anyLong(), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/users/change-password")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }
}
