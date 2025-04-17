package com.hmdp.service.impl;

import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private VoucherMapper voucherMapper;

    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;

    @Autowired
    private VoucherOrderMapper voucherOrderMapper;

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public long seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherMapper.selectById(voucherId);

        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now()) || seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("优惠卷过期");
        }

        if (seckillVoucher.getStock() <= 0) {
            throw new RuntimeException("优惠卷抢光了");
        }

//        seckillVoucher.setStock(seckillVoucher.getStock() - 1);
//        seckillVoucher.setUpdateTime(LocalDateTime.now());
//
//        seckillVoucherMapper.updateById(seckillVoucher);

//        boolean success = seckillVoucherService.update()
//                        .setSql("stock = stock - 1")
//                        .eq("voucher_id", voucherId)
//                        .update();
//
//        if(!success) {
//            throw new RuntimeException("库存不足！");
//        }

//        // TODO 提醒!!!Synchronized加在方法上效率太低，加在方法内部时可能释放了锁但事务还没完成，所以加在调用方法的地方
//        synchronized (UserHolder.getUser().getId().toString().intern()) {
//            // TODO 提醒!!!@Transactional注解是由Spring中的代理对象执行的，但是在这里，如果直接调用createVoucherOrder()方法，则没有使用代理对象，就不会使用@Transactional
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }

//        long userId = UserHolder.getUser().getId();
//
//        // TODO 提醒!!!锁加在方法上效率太低，加在方法内部时可能释放了锁但事务还没完成，所以加在调用方法的地方
//        ILock lock = new RedisLock("order:" + userId, redisTemplate);
//
//        if(lock.tryLock(10)) {
//            try {
//                // TODO 提醒!!!@Transactional注解是由Spring中的代理对象执行的，但是在这里，如果直接调用createVoucherOrder()方法，则没有使用代理对象，就不会使用@Transactional
//                IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//                return proxy.createVoucherOrder(voucherId);
//            } finally {
//                lock.unlock();
//            }
//        }

        long userId = UserHolder.getUser().getId();

        // TODO 提醒!!!锁加在方法上效率太低，加在方法内部时可能释放了锁但事务还没完成，所以加在调用方法的地方
        RLock lock = redissonClient.getLock("lock:order:" + userId);

        try {
            if(lock.tryLock(1, 10, TimeUnit.SECONDS)) {
                // TODO 提醒!!!@Transactional注解是由Spring中的代理对象执行的，但是在这里，如果直接调用createVoucherOrder()方法，则没有使用代理对象，就不会使用@Transactional
                IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
                return proxy.createVoucherOrder(voucherId);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }

        throw new RuntimeException("重复请求！");
    }

    @Transactional
    public long createVoucherOrder(long voucherId) {
        if (voucherOrderMapper.selectOrderNumber(UserHolder.getUser().getId(), voucherId) > 0) {
            throw new RuntimeException("该用户已经下单过此优惠卷，无法再次下单");
        }

//        int count = query()
//                .eq("user_id", UserHolder.getUser().getId())
//                .eq("voucher_id", voucherId)
//                .count();
//
//        if(count > 0) {
//            throw new RuntimeException("该用户已经下单过此优惠卷，无法再次下单");
//        }

        boolean success = seckillVoucherService.soldSeckillVoucherById(voucherId);

        if (!success) {
            throw new RuntimeException("优惠卷抢光了");
        }

        VoucherOrder order = new VoucherOrder();

        long id = redisIdWorker.idWorker("SeckillVoucher");

        order.setId(id);
        order.setVoucherId(voucherId);
        order.setStatus(1);
        order.setUserId(UserHolder.getUser().getId());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        save(order);

        return id;
    }
}
