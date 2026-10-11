package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessScheduleBo;
import org.dromara.fitness.domain.vo.FitnessScheduleVo;

import java.util.Collection;
import java.util.List;

/**
 * 排课 Service 接口
 *
 * @author CodeBuddy
 */
public interface IFitnessScheduleService {

    FitnessScheduleVo queryById(Long scheduleId);

    TableDataInfo<FitnessScheduleVo> queryPageList(FitnessScheduleBo bo, PageQuery pageQuery);

    List<FitnessScheduleVo> queryList(FitnessScheduleBo bo);

    Boolean insertByBo(FitnessScheduleBo bo);

    Boolean updateByBo(FitnessScheduleBo bo);

    Boolean cancelSchedule(Long scheduleId, String cancelReason);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
