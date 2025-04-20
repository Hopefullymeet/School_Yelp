package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IUserService userService;

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

        return redisTemplate.opsForSet().isMember(key, userId);
    }

    /**
     * 点赞博客
     * @param id
     */
    @Override
    public void likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = "blog:liked:" + id;

        boolean isLiked = redisTemplate.opsForSet().isMember(key, userId);

        if(!isLiked) {
            Blog blog = new Blog();
            blog.setId(id);
            boolean isSuccess = blogMapper.likeById(blog);

            if(isSuccess) {
                redisTemplate.opsForSet().add(key, userId);
            }
        } else {
            Blog blog = new Blog();
            blog.setId(id);
            boolean isSuccess = blogMapper.unlikeById(blog);

            if(isSuccess) {
                redisTemplate.opsForSet().remove(key, userId);
            }
        }
    }
}
