package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.Thread;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.apache.tomcat.jni.Thread.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private RedisTemplate redisTemplate;

    private static int counter = 0;

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(10);

    /**
     * 通过id查询shop信息
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
//        Shop shop = queryWithByPass(id);
//        Shop shop = queryWithLock(id);
        Shop shop = queryWithLogicalExpiration(id);

        return Result.ok(shop);
    }

    private Shop queryWithByPass(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;

//        String shopJson = (String) redisTemplate.opsForValue().get(key);
//
//        if(StrUtil.isNotBlank(shopJson)) {
//            return Result.ok(JSONUtil.toBean(shopJson, Shop.class));
//        }
//
//        Shop shop = getById(id);
//
//        if(shop == null) {
//            return Result.fail("不存在对应商家");
//        }
//
//        shopJson = JSONUtil.toJsonStr(shop);
//        redisTemplate.opsForValue().set(key, shopJson);
//
//        return Result.ok(shop);


        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);

//        log.info("{}", map);

        if(!map.isEmpty()) {
            if(map.containsKey("isBlank")) {
                return null;
            }
            return BeanUtil.fillBeanWithMap(map, new Shop(), false);
        }

//        log.info("跳过了");

            Shop shop = getById(id);

            counter++;
            log.info("打到数据库了{}次", counter);

            if(shop == null) {
                map.put("isBlank", true);

                redisTemplate.opsForHash().putAll(key, map);
                redisTemplate.expire(key, RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            Map<String, Object> toStoreMap = BeanUtil.beanToMap(shop);
            redisTemplate.opsForHash().putAll(key, toStoreMap);
            redisTemplate.expire(key, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

            return shop;
    }

    /**
     * 用互斥锁查询
     * @param id
     * @return
     */
    private Shop queryWithLock(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;

//        String shopJson = (String) redisTemplate.opsForValue().get(key);
//
//        if(StrUtil.isNotBlank(shopJson)) {
//            return Result.ok(JSONUtil.toBean(shopJson, Shop.class));
//        }
//
//        Shop shop = getById(id);
//
//        if(shop == null) {
//            return Result.fail("不存在对应商家");
//        }
//
//        shopJson = JSONUtil.toJsonStr(shop);
//        redisTemplate.opsForValue().set(key, shopJson);
//
//        return Result.ok(shop);


        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);

//        log.info("{}", map);

        if(!map.isEmpty()) {
            if(map.containsKey("isBlank")) {
                return null;
            }
            return BeanUtil.fillBeanWithMap(map, new Shop(), false);
        }

//        log.info("跳过了");
        try {
            boolean ifGetLock = tryLock(RedisConstants.LOCK_SHOP_KEY + id);

            if(!ifGetLock) {
                java.lang.Thread.sleep(500);
                return queryWithLock(id);
            }

            Shop shop = getById(id);

            counter++;
            log.info("打到数据库了{}次", counter);

            java.lang.Thread.sleep(200); //模拟读取数据库重建Redis的复杂过程耗时

            if(shop == null) {
                map.put("isBlank", true);

                redisTemplate.opsForHash().putAll(key, map);
                redisTemplate.expire(key, RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            Map<String, Object> toStoreMap = BeanUtil.beanToMap(shop);
            redisTemplate.opsForHash().putAll(key, toStoreMap);
            redisTemplate.expire(key, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

            return shop;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(RedisConstants.LOCK_SHOP_KEY + id);
        }

    }

    /**
     * 用逻辑过期的思路处理缓存击穿
     * @param id
     * @return
     */
    private Shop queryWithLogicalExpiration(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;

//        String shopJson = (String) redisTemplate.opsForValue().get(key);
//
//        if(StrUtil.isNotBlank(shopJson)) {
//            return Result.ok(JSONUtil.toBean(shopJson, Shop.class));
//        }
//
//        Shop shop = getById(id);
//
//        if(shop == null) {
//            return Result.fail("不存在对应商家");
//        }
//
//        shopJson = JSONUtil.toJsonStr(shop);
//        redisTemplate.opsForValue().set(key, shopJson);
//
//        return Result.ok(shop);


        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);

//        log.info("{}", map);

        if(!map.isEmpty()) {
            if(map.containsKey("isBlank")) {
                return null;
            }

            if(LocalDateTime.now().isAfter((LocalDateTime) map.get("expireTime"))) {
                if(tryLock(RedisConstants.LOCK_SHOP_KEY + id)) {
                    EXECUTOR_SERVICE.submit(() -> {
                        try {
                            Shop shop = getById(id);

                            counter++;
                            log.info("打到了数据库{}次", counter);

                            java.lang.Thread.sleep(200);

                            RedisData redisData = new RedisData();
                            redisData.setData(shop);
                            redisData.setExpireTime(LocalDateTime.now().plusMinutes(1));

                            Map<String, Object> toStoreMap = BeanUtil.beanToMap(redisData);
                            redisTemplate.opsForHash().putAll(key, toStoreMap);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } finally {
                            unlock(RedisConstants.LOCK_SHOP_KEY + id);
                        }
                    });
                }
            }
            return (Shop) BeanUtil.fillBeanWithMap(map, new RedisData(), false).getData();
        }

//        log.info("跳过了");

        Shop shop = getById(id);

        counter++;
        log.info("打到数据库了{}次", counter);

        if(shop == null) {
            map.put("isBlank", true);

            redisTemplate.opsForHash().putAll(key, map);
            redisTemplate.expire(key, RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusMinutes(1));

        Map<String, Object> toStoreMap = BeanUtil.beanToMap(redisData);
        redisTemplate.opsForHash().putAll(key, toStoreMap);
//        redisTemplate.expire(key, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return (Shop) redisData.getData();
    }

    /**
     * 获取锁的方法
     * @param key
     * @return
     */
    private boolean tryLock(String key) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(result);
    }

    /**
     * 释放锁的操作
     * @param key
     */
    private void unlock(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 使用SpringCache方法
     * @param id
     * @return
     */
    @Override
    public Result queryByIdTest(Long id) {

        Shop shop = getById(id);

        if(shop == null) {
            return Result.fail("不存在对应商家");
        }

        return Result.ok(shop);
    }

    /**
     * 更新shop
     * @param shop
     * @return
     */
    @Override
    @Transactional
    public void updateShop(Shop shop) {

        if(shop.getId() == null) {
            throw new RuntimeException("shop的id值为null");
        }

        String key = RedisConstants.CACHE_SHOP_KEY + shop.getId();

        updateById(shop);

        redisTemplate.delete(key);
    }
}
