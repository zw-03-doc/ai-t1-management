package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.fitness.domain.FitCoach;
import org.dromara.fitness.domain.bo.FitnessCoachBo;
import org.dromara.fitness.domain.vo.FitnessCoachVo;

/**
 * 教练 Mapper 接口
 *
 * @author CodeBuddy
 */
public interface FitnessCoachMapper extends BaseMapperPlus<FitCoach, FitnessCoachVo> {

    Page<FitnessCoachVo> selectPageCoachList(@Param("page") Page<FitCoach> page, @Param("bo") FitnessCoachBo bo);

    java.util.List<FitnessCoachVo> selectCoachList(@Param("bo") FitnessCoachBo bo);

    FitnessCoachVo selectCoachById(@Param("coachId") Long coachId);

}
