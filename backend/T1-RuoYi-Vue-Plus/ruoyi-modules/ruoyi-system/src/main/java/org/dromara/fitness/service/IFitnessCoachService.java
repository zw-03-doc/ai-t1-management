package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessCoachBo;
import org.dromara.fitness.domain.vo.FitnessCoachVo;

import java.util.Collection;
import java.util.List;

/**
 * 教练 Service 接口
 *
 * @author CodeBuddy
 */
public interface IFitnessCoachService {

    FitnessCoachVo queryById(Long coachId);

    TableDataInfo<FitnessCoachVo> queryPageList(FitnessCoachBo bo, PageQuery pageQuery);

    List<FitnessCoachVo> queryList(FitnessCoachBo bo);

    Boolean insertByBo(FitnessCoachBo bo);

    Boolean updateByBo(FitnessCoachBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
