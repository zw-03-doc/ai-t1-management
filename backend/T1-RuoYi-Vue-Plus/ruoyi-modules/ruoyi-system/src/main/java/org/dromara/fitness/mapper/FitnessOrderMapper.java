package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.fitness.domain.FitOrder;
import org.dromara.fitness.domain.bo.FitnessOrderBo;
import org.dromara.fitness.domain.vo.FitnessOrderItemVo;
import org.dromara.fitness.domain.vo.FitnessOrderVo;

import java.util.List;

/**
 * 订单 Mapper 接口
 *
 * @author CodeBuddy
 */
public interface FitnessOrderMapper extends BaseMapperPlus<FitOrder, FitnessOrderVo> {

    Page<FitnessOrderVo> selectPageOrderList(@Param("page") Page<FitOrder> page,
                                             @Param("bo") FitnessOrderBo bo);

    List<FitnessOrderVo> selectOrderList(@Param("bo") FitnessOrderBo bo);

    FitnessOrderVo selectOrderById(@Param("orderId") Long orderId);

    List<FitnessOrderItemVo> selectOrderItemList(@Param("orderId") Long orderId);
}
