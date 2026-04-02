package com.omnicharge.notification.controller;

import com.omnicharge.notification.dto.NotificationResponse;
import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.entity.NotificationStatus;
import com.omnicharge.notification.entity.NotificationType;
import com.omnicharge.notification.service.INotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
@MockBean(com.omnicharge.common.logging.LogEventPublisher.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private INotificationService notificationService;

    @Test
    void getUserNotifications_Success() throws Exception {
        NotificationResponse resp = NotificationResponse.builder()
                .id(1L).userId(10L).type(NotificationType.EMAIL).category(NotificationCategory.PAYMENT_SUCCESS)
                .subject("Payment Done").message("Success").status(NotificationStatus.SENT)
                .referenceId("TXN-1").isRead(false).createdDate(LocalDateTime.now()).build();

        Page<NotificationResponse> page = new PageImpl<>(Collections.singletonList(resp));
        when(notificationService.getUserNotifications(eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].referenceId").value("TXN-1"));
    }

    @Test
    void markAsRead_Success() throws Exception {
        doNothing().when(notificationService).markAsRead(1L, 10L);

        mockMvc.perform(put("/api/notifications/1/read")
                        .header("X-User-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getUnreadCount_Success() throws Exception {
        when(notificationService.getUnreadCount(10L)).thenReturn(7L);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("X-User-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(7));
    }
}
