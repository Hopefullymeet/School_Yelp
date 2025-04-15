package com.hmdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author lyrics61
 * @version 1.0
 * @since 2025
 */
@Component
public class RedisIdWorker {

    @Autowired
    private RedisTemplate redisTemplate;

    private final static long START_TIMESTAMP = 1735689600L;

    public long idWorker(String keyPrefix) {

        LocalDateTime now = LocalDateTime.now();
        long nowTimestamp = now.toEpochSecond(ZoneOffset.UTC);

        long timestamp = nowTimestamp - START_TIMESTAMP;

        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));

        long count = redisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        return timestamp << 32 | count;
    }
}
