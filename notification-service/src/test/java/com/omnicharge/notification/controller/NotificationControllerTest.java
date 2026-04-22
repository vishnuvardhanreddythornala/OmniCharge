package com.omnicharge.notification.controller;

import com.omnicharge.notification.common.dto.ApiResponse;
import com.omnicharge.notification.dto.NotificationResponse;
import com.omnicharge.notification.service.INotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    void getUserNotifications_Desc() {
        NotificationResponse res = NotificationResponse.builder().id(1L).message("Msg").build();
        Page<NotificationResponse> page = new PageImpl<>(List.of(res));
        when(notificationService.getUserNotifications(eq(1L), any(PageRequest.class))).thenReturn(page);
        
        ResponseEntity<ApiResponse<Page<NotificationResponse>>> response = notificationController.getUserNotifications(
                1L, 0, 10, "createdDate", "DESC");
                
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void getUserNotifications_Asc() {
        NotificationResponse res = NotificationResponse.builder().id(1L).message("Msg").build();
        Page<NotificationResponse> page = new PageImpl<>(List.of(res));
        when(notificationService.getUserNotifications(eq(1L), any(PageRequest.class))).thenReturn(page);
        
        ResponseEntity<ApiResponse<Page<NotificationResponse>>> response = notificationController.getUserNotifications(
                1L, 0, 10, "createdDate", "ASC");
                
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void markAsRead() {
        doNothing().when(notificationService).markAsRead(10L, 1L);
        ResponseEntity<ApiResponse<Void>> response = notificationController.markAsRead(10L, 1L);
        assertEquals(200, response.getStatusCode().value());
        verify(notificationService, times(1)).markAsRead(10L, 1L);
    }

    @Test
    void getUnreadCount() {
        when(notificationService.getUnreadCount(1L)).thenReturn(5L);
        ResponseEntity<ApiResponse<Long>> response = notificationController.getUnreadCount(1L);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(5L, response.getBody().getData());
    }
}
