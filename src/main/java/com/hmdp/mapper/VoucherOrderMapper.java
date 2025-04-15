package com.hmdp.mapper;

import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {

    /**
     * 查看同一个用户下单了几次同一个优惠卷
     * @param userId
     * @param voucherId
     * @return
     */
    @Select("select count(*) from tb_voucher_order where user_id = #{userId} and voucher_id = #{voucherId}")
    int selectOrderNumber(long userId, long voucherId);

}
