package com.omnicharge.notification.dto;

import com.omnicharge.notification.entity.NotificationCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceRequest {

    @NotNull(message = "Category is required")
    private NotificationCategory category;

    @NotNull(message = "Email enabled flag is required")
    private Boolean emailEnabled;

    @NotNull(message = "SMS enabled flag is required")
    private Boolean smsEnabled;

    @NotNull(message = "Enabled flag is required")
    private Boolean isEnabled;
}
