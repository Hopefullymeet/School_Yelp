package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {
    /**
     * 通过id查询shop信息
     * @param id
     * @return
     */
    Result queryById(Long id);

    /**
     * 使用SpringCache方法
     * @param id
     * @return
     */
    Result queryByIdTest(Long id);

    /**
     * 更新shop
     * @param shop
     * @return
     */
    void updateShop(Shop shop);
}
