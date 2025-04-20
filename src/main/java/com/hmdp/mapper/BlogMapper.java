package com.hmdp.mapper;

import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface BlogMapper extends BaseMapper<Blog> {

    /**
     * 通过id增加点赞
     * @param blog
     * @return
     */
    @Update("update tb_blog set liked = liked + 1 where id = #{id}")
    boolean likeById(Blog blog);

    /**
     * 通过id取消点赞
     * @param blog
     * @return
     */
    @Update("update tb_blog set liked = liked - 1 where id = #{id}")
    boolean unlikeById(Blog blog);
}
