package org.dromara.fitness.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.fitness.domain.bo.FitnessHomeBo;
import org.dromara.fitness.domain.vo.FitnessHomeScheduleVo;
import org.dromara.fitness.domain.vo.FitnessHomeStatsVo;

import java.util.List;


/**
 * 健身房 PC 首页 Mapper
 *
 * @author CodeBuddy
 */
public interface FitnessHomeMapper {

    /**
     * 查询首页统计数据
     *
     * @param bo 查询条件
     * @return 首页统计数据
     */
    FitnessHomeStatsVo selectHomeStats(@Param("bo") FitnessHomeBo bo);

    /**
     * 查询今日课程与现场节奏
     *
     * @param bo 查询条件
     * @return 今日课程安排
     */
    List<FitnessHomeScheduleVo> selectTodayScheduleList(@Param("bo") FitnessHomeBo bo);
}
