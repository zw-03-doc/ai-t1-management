package org.dromara.fitness.service;

import org.dromara.fitness.domain.bo.FitnessHomeBo;
import org.dromara.fitness.domain.vo.FitnessHomeOverviewVo;

/**
 * 健身房 PC 首页服务接口
 *
 * @author CodeBuddy
 */
public interface IFitnessHomeService {

    /**
     * 查询首页聚合数据
     *
     * @param bo 查询条件
     * @return 首页数据
     */
    FitnessHomeOverviewVo queryOverview(FitnessHomeBo bo);
}
