package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author lyrics61
 * @version 1.0
 * @since 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisLock implements ILock{
    private static final String KEY_PREFIX = "lock:";

    private static final String UUID_PREFIX = UUID.fastUUID().toString(true) + "-";

    private String key;

    private RedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String value = UUID_PREFIX + Thread.currentThread().getId();

        boolean success = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + key, value, timeoutSec, TimeUnit.SECONDS);

        if(success) {
            return true;
        }
        return false;
    }

    @Override
    public void unlock() {
        redisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + key),
                UUID_PREFIX + Thread.currentThread().getId()
                );
    }

    /*@Override
    public void unlock() {
        String value = (String) redisTemplate.opsForValue().get(KEY_PREFIX + key);

        if(value.equals(UUID_PREFIX + Thread.currentThread().getId())) {
            redisTemplate.delete(KEY_PREFIX + key);
        }
    }*/
}
