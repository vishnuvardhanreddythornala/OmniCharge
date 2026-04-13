package com.omnicharge.user.controller;

import com.omnicharge.user.dto.UserProfileResponse;
import com.omnicharge.user.service.IUserService;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InternalUserController.class, excludeAutoConfiguration = {
        JpaRepositoriesAutoConfiguration.class, DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class InternalUserControllerTest {

    @MockitoBean private JpaMetamodelMappingContext jpaMappingContext;
    @MockitoBean(name = "logEventPublisher") private LogEventPublisher logEventPublisher;
    @Autowired private MockMvc mockMvc;
    @MockitoBean private IUserService userService;

    @Test
    void getUserById_Success() throws Exception {
        UserProfileResponse user = new UserProfileResponse();
        user.setId(1L);
        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getUserByIdInternal_Success() throws Exception {
        UserProfileResponse user = new UserProfileResponse();
        user.setId(2L);
        when(userService.getUserById(2L)).thenReturn(user);

        mockMvc.perform(get("/api/users/internal/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
