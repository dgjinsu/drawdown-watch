package com.example.drawdownwatch.watchlist.controller;

import com.example.drawdownwatch.global.config.SecurityConfig;
import com.example.drawdownwatch.global.exception.GlobalExceptionHandler;
import com.example.drawdownwatch.user.service.JwtTokenProvider;
import com.example.drawdownwatch.watchlist.dto.WatchlistAddRequest;
import com.example.drawdownwatch.watchlist.dto.WatchlistItemResponse;
import com.example.drawdownwatch.watchlist.service.WatchlistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WatchlistController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class WatchlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WatchlistService watchlistService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // -----------------------------------------------------------------------
    // JWT 토큰 스텁 헬퍼: 실제 JWT 토큰 생성 없이 userId를 반환하도록 설정
    // -----------------------------------------------------------------------

    private void stubValidToken(Long userId) {
        given(jwtTokenProvider.validateToken("valid-token")).willReturn(true);
        given(jwtTokenProvider.getUserId("valid-token")).willReturn(userId);
    }

    private WatchlistItemResponse buildWatchlistItemResponse() {
        return new WatchlistItemResponse(
                1L,
                "AAPL",
                "Apple Inc.",
                "US",
                BigDecimal.valueOf(-20.00),
                "52W",
                new BigDecimal("-15.0000"),
                BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(170_000),
                null,
                LocalDateTime.now()
        );
    }

    // -----------------------------------------------------------------------
    // POST /api/watchlist-items
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("워치리스트 항목 추가 성공: JWT 인증 포함 -> 201 Created")
    void addItem_JWT인증포함_201반환() throws Exception {
        // Given
        stubValidToken(1L);
        WatchlistAddRequest request = new WatchlistAddRequest("AAPL", BigDecimal.valueOf(-20.00), "52W");
        WatchlistItemResponse response = buildWatchlistItemResponse();

        given(watchlistService.addItem(eq(1L), any(WatchlistAddRequest.class))).willReturn(response);

        // When / Then
        mockMvc.perform(post("/api/watchlist-items")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.mddPeriod").value("52W"));
    }

    @Test
    @DisplayName("워치리스트 항목 추가 - 인증 없음: 401 Unauthorized")
    void addItem_인증없음_401반환() throws Exception {
        // Given
        WatchlistAddRequest request = new WatchlistAddRequest("AAPL", BigDecimal.valueOf(-20.00), "52W");

        // When / Then
        mockMvc.perform(post("/api/watchlist-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("워치리스트 항목 추가 유효성 실패 - symbol 공백: 400 Bad Request")
    void addItem_symbol공백_400반환() throws Exception {
        // Given
        stubValidToken(1L);
        WatchlistAddRequest request = new WatchlistAddRequest("", BigDecimal.valueOf(-20.00), "52W");

        // When / Then
        mockMvc.perform(post("/api/watchlist-items")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("워치리스트 항목 추가 유효성 실패 - 잘못된 mddPeriod: 400 Bad Request")
    void addItem_잘못된mddPeriod_400반환() throws Exception {
        // Given
        stubValidToken(1L);
        WatchlistAddRequest request = new WatchlistAddRequest("AAPL", BigDecimal.valueOf(-20.00), "INVALID");

        // When / Then
        mockMvc.perform(post("/api/watchlist-items")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // GET /api/watchlist-items
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("워치리스트 목록 조회 성공: JWT 인증 포함 -> 200 OK")
    void getItems_JWT인증포함_200반환() throws Exception {
        // Given
        stubValidToken(1L);
        WatchlistItemResponse item = buildWatchlistItemResponse();
        given(watchlistService.getItems(1L)).willReturn(List.of(item));

        // When / Then
        mockMvc.perform(get("/api/watchlist-items")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("워치리스트 목록 조회 - 인증 없음: 401 Unauthorized")
    void getItems_인증없음_401반환() throws Exception {
        mockMvc.perform(get("/api/watchlist-items"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("워치리스트 빈 목록 조회: 200 OK 빈 배열 반환")
    void getItems_빈목록_200빈배열반환() throws Exception {
        // Given
        stubValidToken(1L);
        given(watchlistService.getItems(1L)).willReturn(List.of());

        // When / Then
        mockMvc.perform(get("/api/watchlist-items")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // -----------------------------------------------------------------------
    // DELETE /api/watchlist-items/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("워치리스트 항목 삭제 성공: JWT 인증 포함 -> 204 No Content")
    void deleteItem_JWT인증포함_204반환() throws Exception {
        // Given
        stubValidToken(1L);

        // When / Then
        mockMvc.perform(delete("/api/watchlist-items/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());

        verify(watchlistService).deleteItem(1L, 1L);
    }

    @Test
    @DisplayName("워치리스트 항목 삭제 - 인증 없음: 401 Unauthorized")
    void deleteItem_인증없음_401반환() throws Exception {
        mockMvc.perform(delete("/api/watchlist-items/1"))
                .andExpect(status().isUnauthorized());
    }
}
