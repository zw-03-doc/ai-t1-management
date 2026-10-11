package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessCourseBo;
import org.dromara.fitness.domain.vo.FitnessCourseVo;

import java.util.Collection;
import java.util.List;

/**
 * 课程 Service 接口
 *
 * @author CodeBuddy
 */
public interface IFitnessCourseService {

    FitnessCourseVo queryById(Long courseId);

    TableDataInfo<FitnessCourseVo> queryPageList(FitnessCourseBo bo, PageQuery pageQuery);

    List<FitnessCourseVo> queryList(FitnessCourseBo bo);

    Boolean insertByBo(FitnessCourseBo bo);

    Boolean updateByBo(FitnessCourseBo bo);

    Boolean updateStatus(Long courseId, String status);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
