package com.devmatch.service;

import com.devmatch.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisLockService {

    private static final String LOCK_PREFIX = "devmatch:lock:";
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(15);
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end",
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    public String tryLockOrThrow(String key) {
        return tryLockOrThrow(key, DEFAULT_TTL);
    }

    public String tryLockOrThrow(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + key, token, ttl != null ? ttl : DEFAULT_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("当前操作正在处理中，请稍后再试");
        }
        return token;
    }

    public void unlock(String key, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    unlockNow(key, token);
                }
            });
            return;
        }
        unlockNow(key, token);
    }

    private void unlockNow(String key, String token) {
        stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(LOCK_PREFIX + key), token);
    }
}
