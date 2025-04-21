package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private IFollowService followService;

    /**
     * 查询是否关注
     * @param id
     * @return
     */
    @GetMapping("/or/not/{id}")
    public Result queryFollowOrNot(@PathVariable Long id) {
        boolean followOrNot = followService.queryFollowOrNot(id);

        return Result.ok(followOrNot);
    }

    /**
     * 关注或取关
     * @param id
     * @param status
     * @return
     */
    @PutMapping("/{id}/{status}")
    public Result followOrUnfollow(@PathVariable(value = "id") Long id, @PathVariable(value = "status") boolean status) {
        log.info("收到的id为：{}，收到的status为：{}", id, status);
        followService.followOrUnfollow(id, status);

        return Result.ok();
    }

}
