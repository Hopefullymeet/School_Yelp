package com.hmdp.service.impl;

import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private VoucherMapper voucherMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private IVoucherOrderService proxy;

    private BlockingQueue<VoucherOrder> queue = new ArrayBlockingQueue<>(1024 * 1024);

    private static final ExecutorService service = Executors.newSingleThreadExecutor();

    private class OrderHandler implements Runnable {
        @Override
        public void run() {
            while(true) {
                try {
                    VoucherOrder voucherOrder = queue.take();

                    handleVoucherOrder(voucherOrder);
                } catch (InterruptedException e) {
                    log.info("异常！");
                }
            }
        }
    }

    @PostConstruct
    public void init() {
        service.submit(new OrderHandler());
    }

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

//        long userId = UserHolder.getUser().getId();
//
//        // TODO 提醒!!!锁加在方法上效率太低，加在方法内部时可能释放了锁但事务还没完成，所以加在调用方法的地方
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//
//        try {
//            if(lock.tryLock(1, 10, TimeUnit.SECONDS)) {
//                // TODO 提醒!!!@Transactional注解是由Spring中的代理对象执行的，但是在这里，如果直接调用createVoucherOrder()方法，则没有使用代理对象，就不会使用@Transactional
//                IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//                return proxy.createVoucherOrder(voucherId);
//            }
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            lock.unlock();
//        }

//        throw new RuntimeException("重复请求！");



        Long userId = UserHolder.getUser().getId();

        // 用StringRedisTemplate！！！！！！！！！！
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), userId.toString());

        int ans = result.intValue();

        if(ans == 1) {
            throw new RuntimeException("库存不足");
        } else if(ans == 2) {
            throw new RuntimeException("用户已经下单过！");
        }

        long orderId = redisIdWorker.idWorker("SeckillVoucher");

        proxy = (IVoucherOrderService) AopContext.currentProxy();

        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(userId);
        voucherOrder.setId(orderId);

        queue.add(voucherOrder);

        return orderId;
    }

    //屎
    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        // 创建锁对象
        RLock redisLock = redissonClient.getLock("lock:order:" + userId);
        // 尝试获取锁
        boolean isLock = redisLock.tryLock();

        if(!isLock) {
            throw new RuntimeException("用户冲突");
        }

        if(isLock) {
            try {
                proxy.createVoucherOrder(voucherOrder);
            } finally {
                redisLock.unlock();
            }
        }
    }

    @Transactional
    @Override
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        if (voucherOrderMapper.selectOrderNumber(voucherOrder.getUserId(), voucherOrder.getVoucherId()) > 0) {
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

        boolean success = seckillVoucherService.soldSeckillVoucherById(voucherOrder.getVoucherId());

        if (!success) {
            throw new RuntimeException("优惠卷抢光了");
        }

        save(voucherOrder);
    }

//    @Transactional
//    @Override
//    public long createVoucherOrder(Long voucherId) {
//        if (voucherOrderMapper.selectOrderNumber(UserHolder.getUser().getId(), voucherId) > 0) {
//            throw new RuntimeException("该用户已经下单过此优惠卷，无法再次下单");
//        }
//
////        int count = query()
////                .eq("user_id", UserHolder.getUser().getId())
////                .eq("voucher_id", voucherId)
////                .count();
////
////        if(count > 0) {
////            throw new RuntimeException("该用户已经下单过此优惠卷，无法再次下单");
////        }
//
//        boolean success = seckillVoucherService.soldSeckillVoucherById(voucherId);
//
//        if (!success) {
//            throw new RuntimeException("优惠卷抢光了");
//        }
//
//        VoucherOrder order = new VoucherOrder();
//
//        long id = redisIdWorker.idWorker("SeckillVoucher");
//
//        order.setId(id);
//        order.setVoucherId(voucherId);
//        order.setStatus(1);
//        order.setUserId(UserHolder.getUser().getId());
//        order.setCreateTime(LocalDateTime.now());
//        order.setUpdateTime(LocalDateTime.now());
//
//        save(order);
//
//        return id;
//    }
}
