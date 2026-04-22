package com.omnicharge.user.controller;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.user.dto.UpdateProfileRequest;
import com.omnicharge.user.dto.UserProfileResponse;
import com.omnicharge.user.service.InterfaceUserService;
import com.omnicharge.user.common.logging.LogEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {
        JpaRepositoriesAutoConfiguration.class, DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @MockitoBean private JpaMetamodelMappingContext jpaMappingContext;
    @MockitoBean(name = "logEventPublisher") private LogEventPublisher logEventPublisher;
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    @MockitoBean private InterfaceUserService userService;


    @Autowired
    public UserControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }
    @Test
    void getProfile_Success() throws Exception {
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(1L);
        profile.setEmail("user@test.com");
        when(userService.getProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/api/users/profile")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateProfile_Success() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("John Doe");

        UserProfileResponse response = new UserProfileResponse();
        response.setId(1L);
        when(userService.updateProfile(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/users/profile")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

}
