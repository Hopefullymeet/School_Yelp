package com.hmdp.service.impl;

import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Autowired
    private FollowMapper followMapper;

    /**
     * 查询是否关注
     * @param id
     * @return
     */
    @Override
    public boolean queryFollowOrNot(Long id) {
        Follow follow = new Follow();
        follow.setUserId(UserHolder.getUser().getId());
        follow.setFollowUserId(id);

        List<Follow> list = followMapper.selectByCon(follow);

        if(list == null || list.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * 关注或取关
     * @param id
     * @param status
     */
    @Override
    public void followOrUnfollow(Long id, boolean status) {
        Follow follow = new Follow();
        follow.setUserId(UserHolder.getUser().getId());
        follow.setFollowUserId(id);
        follow.setCreateTime(LocalDateTime.now());

        if(status) {
            followMapper.insert(follow);
        } else {
            List<Follow> list = followMapper.selectByCon(follow);
            followMapper.deleteById(list.get(0));
        }
    }
}
