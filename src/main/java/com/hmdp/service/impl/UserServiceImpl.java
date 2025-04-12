package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private RedisTemplate redisTemplate;

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
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
//        session.setAttribute("code", code);

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

        String code = (String) redisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + loginForm.getPhone());

        if(!loginForm.getCode().equals(code) || code == null) {
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
        //储存用户信息到redis
//        session.setAttribute("user", userDTO);

        String token = UUID.fastUUID().toString(true);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO);
//        实现讲原有bean中的User的id值转化为String类型
//        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(), CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        redisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + token, map);
        redisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);

        return Result.ok(token);
    }

    private User createUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(5));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        user.setId(Long.valueOf(userMapper.insert(user)));

        return user;
    }
}
