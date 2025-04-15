package com.hmdp.mapper;

import com.hmdp.entity.SeckillVoucher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {
    /**
     * 卖出一个秒杀卷
     * @param id
     */
    @Update("update tb_seckill_voucher set stock = stock - 1 where voucher_id = #{id} and stock > 0")
    boolean soldSeckillVoucherById(long id);
}
