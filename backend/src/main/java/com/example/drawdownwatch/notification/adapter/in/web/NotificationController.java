package com.example.drawdownwatch.notification.adapter.in.web;

import com.example.drawdownwatch.notification.application.port.in.NotificationSettingUseCase;
import com.example.drawdownwatch.notification.application.dto.NotificationSettingRequest;
import com.example.drawdownwatch.notification.application.dto.NotificationSettingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification-settings")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationSettingUseCase notificationSettingService;

    @PostMapping
    public ResponseEntity<NotificationSettingResponse> create(
            @Valid @RequestBody NotificationSettingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationSettingService.create(getCurrentUserId(), request));
    }

    @GetMapping
    public ResponseEntity<List<NotificationSettingResponse>> getAll() {
        return ResponseEntity.ok(notificationSettingService.getAll(getCurrentUserId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationSettingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody NotificationSettingRequest request) {
        return ResponseEntity.ok(notificationSettingService.update(getCurrentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationSettingService.delete(getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<NotificationSettingResponse> toggleEnabled(@PathVariable Long id) {
        return ResponseEntity.ok(notificationSettingService.toggleEnabled(getCurrentUserId(), id));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, String>> test(@PathVariable Long id) {
        String result = notificationSettingService.sendTest(getCurrentUserId(), id);
        return ResponseEntity.ok(Map.of("result", result));
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
