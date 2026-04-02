package com.omnicharge.notification.dto;

import com.omnicharge.notification.entity.NotificationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {

    private Long id;
    private Long userId;
    private NotificationCategory category;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean isEnabled;
}
