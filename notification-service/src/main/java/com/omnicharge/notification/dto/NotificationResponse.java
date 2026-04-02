package com.omnicharge.notification.dto;

import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.entity.NotificationStatus;
import com.omnicharge.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long userId;
    private NotificationType type;
    private NotificationCategory category;
    private String subject;
    private String message;
    private NotificationStatus status;
    private String referenceId;
    private Boolean isRead;
    private LocalDateTime createdDate;
}
