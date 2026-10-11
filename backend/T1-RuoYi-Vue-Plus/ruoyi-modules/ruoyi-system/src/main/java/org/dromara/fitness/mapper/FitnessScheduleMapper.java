package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.fitness.domain.FitCourseSchedule;
import org.dromara.fitness.domain.bo.FitnessScheduleBo;
import org.dromara.fitness.domain.vo.FitnessScheduleVo;

import java.util.List;

/**
 * 排课 Mapper 接口
 *
 * @author CodeBuddy
 */
public interface FitnessScheduleMapper extends BaseMapperPlus<FitCourseSchedule, FitnessScheduleVo> {

    Page<FitnessScheduleVo> selectPageScheduleList(@Param("page") Page<FitCourseSchedule> page,
                                                   @Param("bo") FitnessScheduleBo bo);

    List<FitnessScheduleVo> selectScheduleList(@Param("bo") FitnessScheduleBo bo);

    FitnessScheduleVo selectScheduleById(@Param("scheduleId") Long scheduleId);

    int updateReservedCount(@Param("scheduleId") Long scheduleId, @Param("delta") Integer delta);

    int updateSignedCount(@Param("scheduleId") Long scheduleId, @Param("delta") Integer delta);

    String selectCourseTypeById(@Param("courseId") Long courseId);

    int deleteScheduleStudents(@Param("scheduleId") Long scheduleId);

    int batchInsertScheduleStudents(@Param("scheduleId") Long scheduleId,
                                    @Param("tenantId") String tenantId,
                                    @Param("memberIds") List<Long> memberIds);

    List<Long> selectScheduleStudentIds(@Param("scheduleId") Long scheduleId);
}
