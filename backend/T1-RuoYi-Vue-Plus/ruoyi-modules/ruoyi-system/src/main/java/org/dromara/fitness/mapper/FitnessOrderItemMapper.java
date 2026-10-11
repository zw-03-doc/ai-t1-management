package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.fitness.domain.FitOrderItem;
import org.dromara.fitness.domain.bo.FitnessOrderItemBo;
import org.dromara.fitness.domain.vo.FitnessOrderItemVo;

import java.util.List;

/**
 * 订单明细 Mapper
 *
 * @author CodeBuddy
 */
public interface FitnessOrderItemMapper extends BaseMapperPlus<FitOrderItem, FitnessOrderItemVo> {

    Page<FitnessOrderItemVo> selectPageOrderItemList(@Param("page") Page<FitOrderItem> page,
                                                     @Param("bo") FitnessOrderItemBo bo);

    List<FitnessOrderItemVo> selectOrderItemList(@Param("bo") FitnessOrderItemBo bo);

    FitnessOrderItemVo selectOrderItemById(@Param("itemId") Long itemId);
}
