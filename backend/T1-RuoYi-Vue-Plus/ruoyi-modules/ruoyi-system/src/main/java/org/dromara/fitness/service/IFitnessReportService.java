package org.dromara.fitness.service;

import org.dromara.fitness.domain.bo.FitnessReportBo;
import org.dromara.fitness.domain.vo.FitnessCoachStoreReportVo;
import org.dromara.fitness.domain.vo.FitnessCourseReportVo;
import org.dromara.fitness.domain.vo.FitnessMemberReportVo;
import org.dromara.fitness.domain.vo.FitnessOrderReportVo;

import java.util.List;

/**
 * 报表中心 Service 接口
 *
 * @author CodeBuddy
 */
public interface IFitnessReportService {

    List<FitnessMemberReportVo> queryMemberReportList(FitnessReportBo bo);

    List<FitnessOrderReportVo> queryOrderReportList(FitnessReportBo bo);

    List<FitnessCourseReportVo> queryCourseReportList(FitnessReportBo bo);

    List<FitnessCoachStoreReportVo> queryCoachStoreReportList(FitnessReportBo bo);
}
