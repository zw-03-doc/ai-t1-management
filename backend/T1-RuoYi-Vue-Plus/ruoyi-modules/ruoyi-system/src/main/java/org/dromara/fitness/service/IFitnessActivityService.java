package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessActivityBo;
import org.dromara.fitness.domain.vo.FitnessActivityVo;

import java.util.Collection;
import java.util.List;

/**
 * 活动 Service 接口
 *
 * @author CodeBuddy
 */
public interface IFitnessActivityService {

    FitnessActivityVo queryById(Long activityId);

    TableDataInfo<FitnessActivityVo> queryPageList(FitnessActivityBo bo, PageQuery pageQuery);

    List<FitnessActivityVo> queryList(FitnessActivityBo bo);

    Boolean insertByBo(FitnessActivityBo bo);

    Boolean updateByBo(FitnessActivityBo bo);

    Boolean updateStatus(Long activityId, String status);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
