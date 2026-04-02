package com.omnicharge.notification.entity;

import com.omnicharge.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationTemplate extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private NotificationCategory category;

    @Column(nullable = false, length = 500)
    private String emailSubject;

    @Column(nullable = false, length = 10000)
    private String emailBody;

    @Column(nullable = false, length = 1000)
    private String smsBody;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(length = 1000)
    private String description;
}
