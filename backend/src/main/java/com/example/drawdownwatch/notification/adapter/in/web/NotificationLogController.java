package com.example.drawdownwatch.notification.adapter.in.web;

import com.example.drawdownwatch.notification.application.port.in.NotificationLogUseCase;
import com.example.drawdownwatch.notification.application.dto.NotificationLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/notification-logs")
@RequiredArgsConstructor
public class NotificationLogController {

    private final NotificationLogUseCase notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationLogResponse>> getNotificationLogs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channelType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        Page<NotificationLogResponse> result = notificationService.getNotificationLogs(
                userId, status, channelType, startDate, endDate, PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
