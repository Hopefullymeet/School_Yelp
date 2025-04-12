package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 发送验证码
     * @param phone
     * @param session
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1. 校验手机号
        if(RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号不合法");
        }

        //2. 生成验证码
        String code = RandomUtil.randomNumbers(6);

        //3. 保存验证码
        session.setAttribute("code", code);

        //4. 发送验证码
        log.info("短信验证码为：{}", code);

        return Result.ok();
    }

    /**
     *
     * @param loginForm
     * @param session
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        log.info("{}", loginForm);
        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            return Result.fail("手机号不合法");
        }

        if(!loginForm.getCode().equals(session.getAttribute("code")) || session.getAttribute("code") == null) {
            return Result.fail("验证码错误");
        }

        User toQueryUser = new User();
        toQueryUser.setPhone(loginForm.getPhone());

        User user = userMapper.selectByPhone(toQueryUser);

//        //使用MyBatisPlus的方法
//        User user = query().eq("phone", loginForm.getPhone()).one();

        //若不存在，则创建
        if(user == null) {
            user = createUser(loginForm.getPhone());
        }

        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        //储存用户信息到session
        session.setAttribute("user", userDTO);

        return Result.ok();
    }

    private User createUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(5));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);

        return user;
    }
}
