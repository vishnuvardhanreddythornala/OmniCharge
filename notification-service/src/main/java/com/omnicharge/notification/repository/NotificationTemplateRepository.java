package com.omnicharge.notification.repository;

import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByCategoryAndIsActive(NotificationCategory category, Boolean isActive);

    Optional<NotificationTemplate> findByCategory(NotificationCategory category);
}
