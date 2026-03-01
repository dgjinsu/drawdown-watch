package com.example.drawdownwatch.watchlist.adapter.in.web;

import com.example.drawdownwatch.watchlist.application.dto.WatchlistAddRequest;
import com.example.drawdownwatch.watchlist.application.dto.WatchlistItemResponse;
import com.example.drawdownwatch.watchlist.application.dto.WatchlistUpdateRequest;
import com.example.drawdownwatch.watchlist.application.port.in.WatchlistUseCase;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist-items")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistUseCase watchlistService;

    @PostMapping
    public ResponseEntity<WatchlistItemResponse> addItem(
            @Valid @RequestBody WatchlistAddRequest request) {
        WatchlistItemResponse response = watchlistService.addItem(getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WatchlistItemResponse>> getItems() {
        List<WatchlistItemResponse> response = watchlistService.getItems(getCurrentUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WatchlistItemResponse> getItem(@PathVariable Long id) {
        WatchlistItemResponse response = watchlistService.getItem(getCurrentUserId(), id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WatchlistItemResponse> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody WatchlistUpdateRequest request) {
        WatchlistItemResponse response = watchlistService.updateItem(getCurrentUserId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        watchlistService.deleteItem(getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
