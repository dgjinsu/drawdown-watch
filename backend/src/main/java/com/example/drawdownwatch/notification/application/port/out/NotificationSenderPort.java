package com.example.drawdownwatch.notification.application.port.out;

import com.example.drawdownwatch.notification.domain.NotificationSetting;

public interface NotificationSenderPort {
    boolean supports(String channelType);

    void send(NotificationSetting setting, String userEmail, String subject, String message);
}
