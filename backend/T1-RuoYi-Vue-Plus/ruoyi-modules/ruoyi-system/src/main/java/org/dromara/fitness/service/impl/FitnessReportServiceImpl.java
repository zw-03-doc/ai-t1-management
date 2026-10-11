package org.dromara.fitness.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.fitness.domain.bo.FitnessReportBo;
import org.dromara.fitness.domain.vo.FitnessCoachStoreReportVo;
import org.dromara.fitness.domain.vo.FitnessCourseReportVo;
import org.dromara.fitness.domain.vo.FitnessMemberReportVo;
import org.dromara.fitness.domain.vo.FitnessOrderReportVo;
import org.dromara.fitness.mapper.FitnessReportMapper;
import org.dromara.fitness.service.IFitnessReportService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 报表中心 Service 实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessReportServiceImpl implements IFitnessReportService {

    private final FitnessReportMapper baseMapper;

    @Override
    public List<FitnessMemberReportVo> queryMemberReportList(FitnessReportBo bo) {
        return baseMapper.selectMemberReportList(bo);
    }

    @Override
    public List<FitnessOrderReportVo> queryOrderReportList(FitnessReportBo bo) {
        return baseMapper.selectOrderReportList(bo);
    }

    @Override
    public List<FitnessCourseReportVo> queryCourseReportList(FitnessReportBo bo) {
        return baseMapper.selectCourseReportList(bo);
    }

    @Override
    public List<FitnessCoachStoreReportVo> queryCoachStoreReportList(FitnessReportBo bo) {
        return baseMapper.selectCoachStoreReportList(bo);
    }
}
