package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.fitness.domain.FitActivity;
import org.dromara.fitness.domain.bo.FitnessActivityBo;
import org.dromara.fitness.domain.vo.FitnessActivityVo;

import java.util.List;

/**
 * 活动 Mapper 接口
 *
 * @author CodeBuddy
 */
public interface FitnessActivityMapper extends BaseMapperPlus<FitActivity, FitnessActivityVo> {

    Page<FitnessActivityVo> selectPageActivityList(@Param("page") Page<FitActivity> page, @Param("bo") FitnessActivityBo bo);

    List<FitnessActivityVo> selectActivityList(@Param("bo") FitnessActivityBo bo);

    FitnessActivityVo selectActivityById(@Param("activityId") Long activityId);

    List<Long> selectPlanIdsByActivityId(@Param("activityId") Long activityId);

    int deleteActivityPlanRefs(@Param("activityId") Long activityId);

    int batchInsertActivityPlanRefs(@Param("activityId") Long activityId, @Param("planIds") List<Long> planIds);
}
