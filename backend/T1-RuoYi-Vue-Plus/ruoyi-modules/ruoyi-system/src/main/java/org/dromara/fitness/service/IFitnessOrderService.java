package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessOrderBo;
import org.dromara.fitness.domain.vo.FitnessOrderVo;

import java.util.Collection;
import java.util.List;

/**
 * 订单 Service 接口
 *
 * @author CodeBuddy
 */
public interface IFitnessOrderService {

    FitnessOrderVo queryById(Long orderId);

    TableDataInfo<FitnessOrderVo> queryPageList(FitnessOrderBo bo, PageQuery pageQuery);

    List<FitnessOrderVo> queryList(FitnessOrderBo bo);

    Boolean insertByBo(FitnessOrderBo bo);

    Boolean updateByBo(FitnessOrderBo bo);

    Boolean payOrder(Long orderId, FitnessOrderBo bo);

    Boolean cancelOrder(Long orderId, String cancelReason);

    Boolean refundOrder(Long orderId, FitnessOrderBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
