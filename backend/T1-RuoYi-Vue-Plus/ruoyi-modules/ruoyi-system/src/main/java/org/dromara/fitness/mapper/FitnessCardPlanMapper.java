package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
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

    Page<FitnessCardPlanVo> selectPageCardPlanList(@Param("page") Page<FitCardPlan> page, @Param("bo") FitnessCardPlanBo bo);

    java.util.List<FitnessCardPlanVo> selectCardPlanList(@Param("bo") FitnessCardPlanBo bo);

    FitnessCardPlanVo selectCardPlanById(@Param("planId") Long planId);

}
