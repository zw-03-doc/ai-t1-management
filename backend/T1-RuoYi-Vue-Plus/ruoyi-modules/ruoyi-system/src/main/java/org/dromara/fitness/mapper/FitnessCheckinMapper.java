package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.fitness.domain.FitCheckinRecord;
import org.dromara.fitness.domain.bo.FitnessCheckinBo;
import org.dromara.fitness.domain.vo.FitnessCheckinVo;

import java.util.List;

/**
 * 签到核销 Mapper 接口
 *
 * @author CodeBuddy
 */
public interface FitnessCheckinMapper extends BaseMapperPlus<FitCheckinRecord, FitnessCheckinVo> {

    Page<FitnessCheckinVo> selectPageCheckinList(@Param("page") Page<FitCheckinRecord> page,
                                                 @Param("bo") FitnessCheckinBo bo);

    List<FitnessCheckinVo> selectCheckinList(@Param("bo") FitnessCheckinBo bo);

    FitnessCheckinVo selectCheckinById(@Param("checkinId") Long checkinId);
}
