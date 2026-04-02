package com.omnicharge.notification.repository;

import com.omnicharge.notification.entity.NotificationCategory;
import com.omnicharge.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUserIdAndCategory(Long userId, NotificationCategory category);

    List<NotificationPreference> findByUserId(Long userId);

    boolean existsByUserIdAndCategory(Long userId, NotificationCategory category);
}
