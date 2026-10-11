package org.dromara.fitness.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessOrderItemBo;
import org.dromara.fitness.domain.vo.FitnessOrderItemVo;
import org.dromara.fitness.mapper.FitnessOrderItemMapper;
import org.dromara.fitness.service.IFitnessOrderItemService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单明细 Service 实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessOrderItemServiceImpl implements IFitnessOrderItemService {

    private final FitnessOrderItemMapper baseMapper;

    @Override
    public FitnessOrderItemVo queryById(Long itemId) {
        return baseMapper.selectOrderItemById(itemId);
    }

    @Override
    public TableDataInfo<FitnessOrderItemVo> queryPageList(FitnessOrderItemBo bo, PageQuery pageQuery) {
        return TableDataInfo.build(baseMapper.selectPageOrderItemList(pageQuery.build(), bo));
    }

    @Override
    public List<FitnessOrderItemVo> queryList(FitnessOrderItemBo bo) {
        return baseMapper.selectOrderItemList(bo);
    }
}
