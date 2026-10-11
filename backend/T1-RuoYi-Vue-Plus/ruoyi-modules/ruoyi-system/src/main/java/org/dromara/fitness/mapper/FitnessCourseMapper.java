package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.fitness.domain.FitCourse;
import org.dromara.fitness.domain.bo.FitnessCourseBo;
import org.dromara.fitness.domain.vo.FitnessCourseVo;

/**
 * 课程 Mapper 接口
 *
 * @author CodeBuddy
 */
public interface FitnessCourseMapper extends BaseMapperPlus<FitCourse, FitnessCourseVo> {

    Page<FitnessCourseVo> selectPageCourseList(@Param("page") Page<FitCourse> page, @Param("bo") FitnessCourseBo bo);

    java.util.List<FitnessCourseVo> selectCourseList(@Param("bo") FitnessCourseBo bo);

    FitnessCourseVo selectCourseById(@Param("courseId") Long courseId);

}
