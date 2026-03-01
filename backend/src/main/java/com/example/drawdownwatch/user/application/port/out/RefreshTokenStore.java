package com.example.drawdownwatch.user.application.port.out;

import java.util.Optional;

public interface RefreshTokenStore {

    void save(String token, Long userId, long ttlMillis);

    Optional<Long> findUserIdByToken(String token);

    void delete(String token);

    void deleteAllByUserId(Long userId);
}
