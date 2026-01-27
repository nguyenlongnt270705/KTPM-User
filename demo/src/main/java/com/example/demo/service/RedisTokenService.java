package com.example.demo.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final RedisTemplate<String, Object> redisTemplate;


    public void saveToken(String username, String token, long durationMinutes) {
//        redisTemplate.opsForValue().set("TOKEN:" + username, token, durationMinutes, TimeUnit.MINUTES);
        String key = "TOKEN:" + username;
        redisTemplate.execute(new SessionCallback<>() {
            @SuppressWarnings("unchecked")
            @Override
            public Object execute(@NonNull RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.delete(key); // xóa token cũ nếu có
                operations.opsForValue().set(key, token, durationMinutes, TimeUnit.MINUTES);
                return operations.exec();
            }
        });
    }

    public String getToken(String username) {
        return (String) redisTemplate.opsForValue().get("TOKEN:" + username);
    }

    public boolean isTokenValid(String username, String token) {

        return redisTemplate.hasKey("TOKEN:" + username) && Objects.equals(redisTemplate.opsForValue().get("TOKEN:" + username), token);
    }

    public void checkTokenExisted(String username, String token, long durationMinutes) {
        if (redisTemplate.hasKey("TOKEN:" + username)) {
            redisTemplate.delete("TOKEN:" + username);
            redisTemplate.opsForValue().set("TOKEN:" + username, token, durationMinutes, TimeUnit.MINUTES);
        }
    }

    public void deleteToken(String jwtId) {
        redisTemplate.delete("TOKEN:" + jwtId);
    }

    public void saveRefreshToken(String username, String refreshToken, long durationMinutes) {
        String key = "REFRESH_TOKEN:" + username;
        redisTemplate.execute(new SessionCallback<>() {
            @SuppressWarnings("unchecked")
            @Override
            public Object execute(@NonNull RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.delete(key); // xóa refresh token cũ nếu có
                operations.opsForValue().set(key, refreshToken, durationMinutes, TimeUnit.MINUTES);
                return operations.exec();
            }
        });
    }

    public String getRefreshToken(String username) {
        return (String) redisTemplate.opsForValue().get("REFRESH_TOKEN:" + username);
    }

    public boolean isRefreshTokenValid(String username, String refreshToken) {
        return redisTemplate.hasKey("REFRESH_TOKEN:" + username)
                && Objects.equals(redisTemplate.opsForValue().get("REFRESH_TOKEN:" + username), refreshToken);
    }

    public void deleteRefreshToken(String username) {
        redisTemplate.delete("REFRESH_TOKEN:" + username);
    }
}
