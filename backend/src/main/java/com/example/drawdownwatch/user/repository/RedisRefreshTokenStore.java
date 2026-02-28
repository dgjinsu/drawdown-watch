package com.example.drawdownwatch.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String TOKEN_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String token, Long userId, long ttlMillis) {
        String tokenKey = TOKEN_PREFIX + token;
        String userKey = USER_TOKENS_PREFIX + userId;

        redisTemplate.opsForValue().set(tokenKey, String.valueOf(userId), ttlMillis, TimeUnit.MILLISECONDS);
        redisTemplate.opsForSet().add(userKey, token);
        redisTemplate.expire(userKey, ttlMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public Optional<Long> findUserIdByToken(String token) {
        String value = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(value));
    }

    @Override
    public void delete(String token) {
        String userId = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        redisTemplate.delete(TOKEN_PREFIX + token);
        if (userId != null) {
            redisTemplate.opsForSet().remove(USER_TOKENS_PREFIX + userId, token);
        }
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        String userKey = USER_TOKENS_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userKey);
        if (tokens != null && !tokens.isEmpty()) {
            Set<String> tokenKeys = new java.util.HashSet<>();
            for (String token : tokens) {
                tokenKeys.add(TOKEN_PREFIX + token);
            }
            redisTemplate.delete(tokenKeys);
        }
        redisTemplate.delete(userKey);
    }
}
