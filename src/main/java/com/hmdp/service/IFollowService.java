package com.hmdp.service;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.User;

import java.util.List;

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

    /**
     * 获取共同关注列表
     * @param id
     * @return
     */
    List<UserDTO> getCommonFollow(Long id);

    /**
     * 通过被关注者id筛选粉丝
     * @param id
     * @return
     */
    List<Follow> getFansByFollowId(Long id);
}
