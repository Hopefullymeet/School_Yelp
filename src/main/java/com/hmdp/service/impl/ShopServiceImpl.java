package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    /**
     * 通过id查询shop信息
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {

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

        log.info("{}", map);

        if(!map.isEmpty()) {
            if(map.containsKey("isBlank")) {
                return Result.fail("不存在此值");
            }
            return Result.ok(BeanUtil.fillBeanWithMap(map, new Shop(), false));
        }

        log.info("跳过了");

        Shop shop = getById(id);

        if(shop == null) {

            map.put("isBlank", true);

            redisTemplate.opsForHash().putAll(key, map);
            redisTemplate.expire(key, RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("不存在此值");
        }

        Map<String, Object> toStoreMap = BeanUtil.beanToMap(shop);
        redisTemplate.opsForHash().putAll(key, toStoreMap);
        redisTemplate.expire(key, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return Result.ok(shop);
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
