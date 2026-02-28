package com.example.drawdownwatch.notification.repository;

import com.example.drawdownwatch.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    List<NotificationSetting> findByUserIdAndEnabledTrue(Long userId);

    Optional<NotificationSetting> findByUserIdAndChannelType(Long userId, String channelType);

    List<NotificationSetting> findAllByUserId(Long userId);
}
