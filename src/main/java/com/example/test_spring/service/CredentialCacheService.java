package com.example.test_spring.service;

import com.example.test_spring.DTO.Both.CredentialCacheDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialCacheService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.credential-cache-ttl-minutes}")
    private long ttlMinutes;

    private String key(String email) {
        return "credential:" + email.toLowerCase();
    }

    public Optional<CredentialCacheDTO> get(String email) {
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key(email));
        if (data.isEmpty()) return Optional.empty();

        CredentialCacheDTO cred = new CredentialCacheDTO();
        cred.setIdUser((String) data.get("userId"));
        cred.setEmail((String) data.get("email"));
        cred.setPassword((String) data.get("passwordHash"));
        cred.setRole((String) data.get("role"));

        if (cred.getIdUser() == null || cred.getPassword() == null || cred.getRole() == null) {
            redisTemplate.delete(key(email));
            return Optional.empty();
        }
        return Optional.of(cred);
    }

    public void put(CredentialCacheDTO cred) {
        String key = key(cred.getEmail());
        redisTemplate.opsForHash().putAll(key, Map.of(
                "userId", cred.getIdUser(),
                "email", cred.getEmail(),
                "passwordHash", cred.getPassword(),
                "role", cred.getRole()));
        redisTemplate.expire(key, Duration.ofMinutes(ttlMinutes));
    }

    public void evict(String email) {
        redisTemplate.delete(key(email));
    }
}