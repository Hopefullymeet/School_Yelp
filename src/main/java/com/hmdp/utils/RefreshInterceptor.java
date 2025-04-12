package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author lyrics61
 * @version 1.0
 * @since 2025
 */
public class RefreshInterceptor implements HandlerInterceptor {

    private RedisTemplate redisTemplate;

    public RefreshInterceptor(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = RedisConstants.LOGIN_USER_KEY + request.getHeader("authorization");

        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);

        if(map.isEmpty()) {
            return true;
        }

        UserDTO user = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);

        redisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);

        UserHolder.saveUser(user);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
