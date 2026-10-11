package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessCardPlanBo;
import org.dromara.fitness.domain.vo.FitnessCardPlanVo;

import java.util.Collection;
import java.util.List;

/**
 * 卡项方案 Service 接口
 *
 * @author CodeBuddy
 */
public interface IFitnessCardPlanService {

    FitnessCardPlanVo queryById(Long planId);

    TableDataInfo<FitnessCardPlanVo> queryPageList(FitnessCardPlanBo bo, PageQuery pageQuery);

    List<FitnessCardPlanVo> queryList(FitnessCardPlanBo bo);

    Boolean insertByBo(FitnessCardPlanBo bo);

    Boolean updateByBo(FitnessCardPlanBo bo);

    Boolean updateStatus(Long planId, String status);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
