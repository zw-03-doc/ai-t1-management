package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessCoachAppBo;
import org.dromara.fitness.domain.vo.FitnessCoachAppVo;

/**
 * 教练 App 服务接口
 *
 * @author CodeBuddy
 */
public interface IFitnessCoachAppService {

    FitnessCoachAppVo.LoginVo login(FitnessCoachAppBo.LoginBo bo);

    FitnessCoachAppVo.LoginVo wechatLogin(FitnessCoachAppBo.WechatLoginBo bo);

    FitnessCoachAppVo.LoginVo wechatActivate(FitnessCoachAppBo.WechatActivateBo bo);

    void logout();

    FitnessCoachAppVo.HomeOverviewVo queryHomeOverview();

    TableDataInfo<FitnessCoachAppVo.ScheduleVo> queryMySchedule(FitnessCoachAppBo.ScheduleQueryBo bo, PageQuery pageQuery);

    FitnessCoachAppVo.ScheduleVo queryScheduleDetail(Long scheduleId);

    TableDataInfo<FitnessCoachAppVo.CheckinVo> queryCheckinList(FitnessCoachAppBo.CheckinQueryBo bo, PageQuery pageQuery);

    FitnessCoachAppVo.CheckinVo queryCheckinDetail(Long reservationId);

    FitnessCoachAppVo.CheckinVo confirmCheckin(FitnessCoachAppBo.CheckinConfirmBo bo);

    FitnessCoachAppVo.CheckinVo markCheckinException(FitnessCoachAppBo.CheckinExceptionBo bo);

    TableDataInfo<FitnessCoachAppVo.StudentVo> queryMyStudents(String keyword, PageQuery pageQuery);

    FitnessCoachAppVo.StudentVo queryStudentDetail(Long memberId);

    FitnessCoachAppVo.CoachProfileVo queryProfile();

    void updateProfile(FitnessCoachAppBo.ProfileUpdateBo bo);

    void updatePassword(FitnessCoachAppBo.PasswordUpdateBo bo);

    TableDataInfo<FitnessCoachAppVo.MessageVo> queryMessageList(PageQuery pageQuery);
}
