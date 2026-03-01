package com.example.drawdownwatch.notification.application.port.in;

import com.example.drawdownwatch.notification.application.dto.NotificationSettingRequest;
import com.example.drawdownwatch.notification.application.dto.NotificationSettingResponse;

import java.util.List;

public interface NotificationSettingUseCase {

    NotificationSettingResponse create(Long userId, NotificationSettingRequest request);

    List<NotificationSettingResponse> getAll(Long userId);

    NotificationSettingResponse update(Long userId, Long settingId, NotificationSettingRequest request);

    void delete(Long userId, Long settingId);

    NotificationSettingResponse toggleEnabled(Long userId, Long settingId);

    String sendTest(Long userId, Long settingId);
}
