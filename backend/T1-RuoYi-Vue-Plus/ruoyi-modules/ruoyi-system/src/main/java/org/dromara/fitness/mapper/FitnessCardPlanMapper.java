package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import org.dromara.fitness.domain.vo.FitnessPlanCourseVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.fitness.domain.FitCardPlan;
import org.dromara.fitness.domain.bo.FitnessCardPlanBo;
import org.dromara.fitness.domain.vo.FitnessCardPlanVo;

/**
 * 卡项方案 Mapper 接口
 *
 * @author CodeBuddy
 */
public interface FitnessCardPlanMapper extends BaseMapperPlus<FitCardPlan, FitnessCardPlanVo> {

    List<FitnessPlanCourseVo> selectExtraCourses(@Param("tenant") String tenant, @Param("planIds") List<Long> planIds,
                                                @Param("publishedOnly") boolean publishedOnly);

    boolean hasExtraCourse(@Param("tenant") String tenant, @Param("planId") Long planId, @Param("courseId") Long courseId);

    int deleteExtraCourses(@Param("tenant") String tenant, @Param("planId") Long planId);

    int insertExtraCourses(@Param("tenant") String tenant, @Param("planId") Long planId, @Param("ids") List<Long> ids);

    Page<FitnessCardPlanVo> selectPageCardPlanList(@Param("page") Page<FitCardPlan> page, @Param("bo") FitnessCardPlanBo bo);

    java.util.List<FitnessCardPlanVo> selectCardPlanList(@Param("bo") FitnessCardPlanBo bo);

    FitnessCardPlanVo selectCardPlanById(@Param("planId") Long planId);

}
