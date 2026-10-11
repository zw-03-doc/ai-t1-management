package org.dromara.fitness.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.fitness.domain.bo.FitnessReportBo;
import org.dromara.fitness.domain.vo.FitnessCoachStoreReportVo;
import org.dromara.fitness.domain.vo.FitnessCourseReportVo;
import org.dromara.fitness.domain.vo.FitnessMemberReportVo;
import org.dromara.fitness.domain.vo.FitnessOrderReportVo;

import java.util.List;

/**
 * 报表中心 Mapper
 *
 * @author CodeBuddy
 */
public interface FitnessReportMapper {

    List<FitnessMemberReportVo> selectMemberReportList(@Param("bo") FitnessReportBo bo);

    List<FitnessOrderReportVo> selectOrderReportList(@Param("bo") FitnessReportBo bo);

    List<FitnessCourseReportVo> selectCourseReportList(@Param("bo") FitnessReportBo bo);

    List<FitnessCoachStoreReportVo> selectCoachStoreReportList(@Param("bo") FitnessReportBo bo);
}
