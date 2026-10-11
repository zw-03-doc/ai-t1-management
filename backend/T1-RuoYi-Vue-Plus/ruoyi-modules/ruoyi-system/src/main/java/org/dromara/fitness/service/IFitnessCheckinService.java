package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessCheckinBo;
import org.dromara.fitness.domain.vo.FitnessCheckinVo;

import java.util.List;

/**
 * 签到核销 Service 接口
 *
 * @author CodeBuddy
 */
public interface IFitnessCheckinService {

    FitnessCheckinVo queryById(Long checkinId);

    TableDataInfo<FitnessCheckinVo> queryPageList(FitnessCheckinBo bo, PageQuery pageQuery);

    List<FitnessCheckinVo> queryList(FitnessCheckinBo bo);

    Boolean confirmCheckin(Long checkinId, FitnessCheckinBo bo);

    Boolean markCheckinException(Long checkinId, String remark);
}
