package com.example.drawdownwatch.watchlist.application.port.in;

import com.example.drawdownwatch.watchlist.application.dto.WatchlistAddRequest;
import com.example.drawdownwatch.watchlist.application.dto.WatchlistItemResponse;
import com.example.drawdownwatch.watchlist.application.dto.WatchlistUpdateRequest;

import java.util.List;

public interface WatchlistUseCase {

    WatchlistItemResponse addItem(Long userId, WatchlistAddRequest request);

    List<WatchlistItemResponse> getItems(Long userId);

    WatchlistItemResponse getItem(Long userId, Long itemId);

    WatchlistItemResponse updateItem(Long userId, Long itemId, WatchlistUpdateRequest request);

    void deleteItem(Long userId, Long itemId);
}
