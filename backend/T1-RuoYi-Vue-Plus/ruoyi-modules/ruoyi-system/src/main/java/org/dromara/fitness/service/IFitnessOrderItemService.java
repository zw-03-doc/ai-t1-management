package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessOrderItemBo;
import org.dromara.fitness.domain.vo.FitnessOrderItemVo;

import java.util.List;

/**
 * 订单明细 Service 接口
 *
 * @author CodeBuddy
 */
public interface IFitnessOrderItemService {

    FitnessOrderItemVo queryById(Long itemId);

    TableDataInfo<FitnessOrderItemVo> queryPageList(FitnessOrderItemBo bo, PageQuery pageQuery);

    List<FitnessOrderItemVo> queryList(FitnessOrderItemBo bo);
}
