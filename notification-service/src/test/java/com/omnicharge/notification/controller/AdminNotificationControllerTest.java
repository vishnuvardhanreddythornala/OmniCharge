package com.omnicharge.notification.controller;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.omnicharge.notification.common.logging.LogEventPublisher;
import com.omnicharge.notification.dto.NotificationResponse;
import com.omnicharge.notification.service.INotificationService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(controllers = AdminNotificationController.class, excludeAutoConfiguration = {
        JpaRepositoriesAutoConfiguration.class, DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminNotificationControllerTest {

    @MockitoBean private JpaMetamodelMappingContext jpaMappingContext;
    @MockitoBean(name = "logEventPublisher") private LogEventPublisher logEventPublisher;
    private final MockMvc mockMvc;
    @MockitoBean private INotificationService notificationService;

    @Autowired
    public AdminNotificationControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void getAllNotifications_Success() throws Exception {
        Page<NotificationResponse> page = new PageImpl<>(Collections.emptyList());
        when(notificationService.getAllNotifications(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/notifications")
                .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAllNotifications_AscSort() throws Exception {
        Page<NotificationResponse> page = new PageImpl<>(Collections.emptyList());
        when(notificationService.getAllNotifications(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/notifications")
                .param("sortDir", "ASC"))
                .andExpect(status().isOk());
    }
}
