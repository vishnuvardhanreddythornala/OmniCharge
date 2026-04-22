package com.omnicharge.user.controller;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = AdminUserController.class, excludeAutoConfiguration = {
        JpaRepositoriesAutoConfiguration.class, DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @MockitoBean private JpaMetamodelMappingContext jpaMappingContext;
    @MockitoBean(name = "logEventPublisher") private LogEventPublisher logEventPublisher;
    private final MockMvc mockMvc;
    @MockitoBean private InterfaceUserService userService;


    @Autowired
    public AdminUserControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }
    @Test
    void getAllUsers_Success() throws Exception {
        UserProfileResponse user = new UserProfileResponse();
        user.setId(1L);
        user.setEmail("user@test.com");
        Page<UserProfileResponse> page = new PageImpl<>(List.of(user));

        when(userService.getAllUsers(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAllUsers_AscSort() throws Exception {
        Page<UserProfileResponse> page = new PageImpl<>(List.of());
        when(userService.getAllUsers(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users")
                .param("sortDir", "ASC"))
                .andExpect(status().isOk());
    }

    @Test
    void getUserById_Success() throws Exception {
        UserProfileResponse user = new UserProfileResponse();
        user.setId(1L);
        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void toggleUserStatus_Success() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/status")
                .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
