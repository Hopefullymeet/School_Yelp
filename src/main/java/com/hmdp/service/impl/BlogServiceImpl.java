package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

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
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IUserService userService;

    @Autowired
    private IFollowService followService;

    /**
     * 根据id查询博客
     * @param id
     * @return
     */
    @Override
    public Blog selectBlog(Long id) {
        Blog blog = blogMapper.selectById(id);
        if(isLiked(blog)) {
            blog.setIsLike(true);
        }

        return blog;
    }

    /**
     * 分页查询
     * @param current
     * @return
     */
    @Override
    public List<Blog> queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
            if(isLiked(blog)) {
                blog.setIsLike(true);
            }
        });

        return records;
    }

    private boolean isLiked(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        String key = "blog:liked:" + blog.getId();

        return stringRedisTemplate.opsForZSet().score(key, userId.toString()) != null;
    }

    /**
     * 点赞博客
     * @param id
     */
    @Override
    public void likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = "blog:liked:" + id;

//        boolean isLiked = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        Double isLiked = stringRedisTemplate.opsForZSet().score(key, userId.toString());

        if(isLiked == null) {
            Blog blog = new Blog();
            blog.setId(id);
            boolean isSuccess = blogMapper.likeById(blog);

            if(isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            Blog blog = new Blog();
            blog.setId(id);
            boolean isSuccess = blogMapper.unlikeById(blog);

            if(isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
    }

    /**
     * 查询点赞时间前N名
     * @param id
     * @return
     */
    @Override
    public List<UserDTO> queryTopNLike(Long id) {
        List<UserDTO> ans = new ArrayList<>();
        String key = "blog:liked:" + id;

        Set<String> range = stringRedisTemplate.opsForZSet().range(key, 0, 5);

        if(range == null || range.isEmpty()) {
            return ans;
        }

        for(String o : range) {
            UserDTO userDTO = new UserDTO();

            Long userId = Long.parseLong(o);
            BeanUtils.copyProperties(userService.getById(userId), userDTO);

            ans.add(userDTO);
        }

        return ans;
    }

    /**
     * 保存博客
     * @param blog
     */
    @Override
    public void saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        save(blog);

        Long blogId = blog.getId();
        List<Follow> followList = followService.getFansByFollowId(user.getId());

        for(Follow follow : followList) {
            String key = "inbox:user:" + follow.getUserId();
            stringRedisTemplate.opsForZSet().add(key, blogId.toString(), System.currentTimeMillis());
        }
    }

    /**
     * 获取关注者的博客
     * @param lastId
     * @return
     */
    @Override
    public ScrollResult getFollowBlog(Long lastId, Integer offset) {
        ScrollResult ans = new ScrollResult();
        List<Blog> blogList = new ArrayList<>();
        Integer toSetOffset = 0;

        String key = "inbox:user:" + UserHolder.getUser().getId();

        if(offset == null) {
            offset = 0;
        }
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, lastId, offset, 3);

        Long minTime = System.currentTimeMillis();
        for(ZSetOperations.TypedTuple typedTuple : typedTuples) {
            blogList.add(selectBlog(Long.valueOf((String) typedTuple.getValue())));
            minTime = Math.min(minTime, typedTuple.getScore().longValue());
        }

        for(ZSetOperations.TypedTuple typedTuple : typedTuples) {
            if(Long.valueOf(typedTuple.getScore().longValue()).equals(minTime)) {
                toSetOffset++;
            }
        }

        ans.setList(blogList);
        ans.setMinTime(minTime);
        ans.setOffset(toSetOffset);

        return ans;
    }
}
