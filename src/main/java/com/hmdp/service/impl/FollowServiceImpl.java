package com.hmdp.service.impl;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IUserService userService;

    /**
     * 查询是否关注
     * @param id
     * @return
     */
    @Override
    public boolean queryFollowOrNot(Long id) {
        /*Follow follow = new Follow();
        follow.setUserId(UserHolder.getUser().getId());
        follow.setFollowUserId(id);

        List<Follow> list = followMapper.selectByCon(follow);

        if(list == null || list.isEmpty()) {
            return false;
        }

        return true;*/

        String key = "follow:user:" + UserHolder.getUser().getId();
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(key, id.toString()));
    }

    /**
     * 关注或取关
     * @param id
     * @param status
     */
    @Override
    public void followOrUnfollow(Long id, boolean status) {
        /*Follow follow = new Follow();
        follow.setUserId(UserHolder.getUser().getId());
        follow.setFollowUserId(id);
        follow.setCreateTime(LocalDateTime.now());

        if(status) {
            followMapper.insert(follow);
        } else {
            List<Follow> list = followMapper.selectByCon(follow);
            followMapper.deleteById(list.get(0));
        }*/

        String key = "follow:user:" + UserHolder.getUser().getId();
        if(status) {
            stringRedisTemplate.opsForSet().add(key, id.toString());
        } else {
            stringRedisTemplate.opsForSet().remove(key, id.toString());
        }
    }

    /**
     * 获取共同关注列表
     * @param id
     * @return
     */
    @Override
    public List<UserDTO> getCommonFollow(Long id) {
        List<UserDTO> list = new ArrayList<>();

        String keyMe = "follow:user:" + UserHolder.getUser().getId();
        String keyCommon = "follow:user:" + id;

        Set<String> idsStr = stringRedisTemplate.opsForSet().intersect(keyMe, keyCommon);

        for(String idStr : idsStr) {
            User user = userService.getById(Long.parseLong(idStr));
            UserDTO userDTO = new UserDTO();

            BeanUtils.copyProperties(user, userDTO);
            list.add(userDTO);
        }

        return list;
    }
}
