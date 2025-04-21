package com.hmdp.service;

import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {
    /**
     * 查询是否关注
     * @param id
     * @return
     */
    boolean queryFollowOrNot(Long id);

    /**
     * 关注或取关
     * @param id
     * @param status
     */
    void followOrUnfollow(Long id, boolean status);
}
