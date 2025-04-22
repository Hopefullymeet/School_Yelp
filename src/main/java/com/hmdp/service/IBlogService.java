package com.hmdp.service;

import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {
    /**
     * 通过id查询博客
     * @param id
     * @return
     */
    Blog selectBlog(Long id);

    /**
     * 点赞博客
     * @param id
     */
    void likeBlog(Long id);

    /**
     * 首页分页查询
     * @param current
     * @return
     */
    List<Blog> queryHotBlog(Integer current);

    /**
     * 查询点赞时间前N名
     * @param id
     * @return
     */
    List<UserDTO> queryTopNLike(Long id);

    /**
     * 保存博客
     * @param blog
     */
    void saveBlog(Blog blog);

    /**
     * 获取关注者的博客
     * @param lastId
     * @return
     */
    ScrollResult getFollowBlog(Long lastId, Integer offset);
}
