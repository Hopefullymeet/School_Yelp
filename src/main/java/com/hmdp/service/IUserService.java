package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {
    /**
     * 发送验证码
     * @param phone
     * @param session
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 登录
     * @param loginForm
     * @param session
     */
    Result login(LoginFormDTO loginForm, HttpSession session);

    /**
     * 签到
     * @return
     */
    void sign();

    /**
     * 计算本月连续签到天数
     * @return
     */
    Integer getCount();
}
