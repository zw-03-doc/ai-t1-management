package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.fitness.domain.FitCourseReservation;
import org.dromara.fitness.domain.bo.FitnessReservationBo;
import org.dromara.fitness.domain.vo.FitnessReservationVo;

import java.util.List;

/**
 * 预约 Mapper 接口
 *
 * @author CodeBuddy
 */
public interface FitnessReservationMapper extends BaseMapperPlus<FitCourseReservation, FitnessReservationVo> {

    Page<FitnessReservationVo> selectPageReservationList(@Param("page") Page<FitCourseReservation> page,
                                                         @Param("bo") FitnessReservationBo bo);

    List<FitnessReservationVo> selectReservationList(@Param("bo") FitnessReservationBo bo);

    FitnessReservationVo selectReservationById(@Param("reservationId") Long reservationId);
}
