package org.dromara.fitness.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.fitness.domain.bo.FitnessCoachAppBo;
import org.dromara.fitness.domain.vo.FitnessCoachAppVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 教练 App Mapper
 *
 * @author CodeBuddy
 */
public interface FitnessCoachAppMapper {

    FitnessCoachAppVo.CoachProfileVo selectCoachAuthInfo(@Param("username") String username);

    FitnessCoachAppVo.CoachProfileVo selectCoachAuthInfoByCoachId(@Param("coachId") Long coachId);

    FitnessCoachAppVo.CoachProfileVo selectCoachProfileByUserId(@Param("userId") Long userId);

    FitnessCoachAppVo.HomeStatsVo selectCoachHomeStats(@Param("coachId") Long coachId);

    List<FitnessCoachAppVo.ScheduleVo> selectCoachScheduleList(@Param("coachId") Long coachId,
                                                               @Param("bo") FitnessCoachAppBo.ScheduleQueryBo bo);

    FitnessCoachAppVo.ScheduleVo selectCoachScheduleDetail(@Param("coachId") Long coachId,
                                                           @Param("scheduleId") Long scheduleId);

    List<FitnessCoachAppVo.CheckinVo> selectCoachCheckinList(@Param("coachId") Long coachId,
                                                             @Param("bo") FitnessCoachAppBo.CheckinQueryBo bo);

    FitnessCoachAppVo.CheckinVo selectCoachCheckinDetail(@Param("coachId") Long coachId,
                                                         @Param("reservationId") Long reservationId);

    FitnessCoachAppVo.CheckinActionVo selectCoachCheckinAction(@Param("coachId") Long coachId,
                                                               @Param("reservationId") Long reservationId);

    int updateCheckinConfirm(@Param("checkinId") Long checkinId,
                             @Param("operatorUserId") Long operatorUserId,
                             @Param("consumeTimes") Integer consumeTimes,
                             @Param("consumeAmount") BigDecimal consumeAmount,
                             @Param("remark") String remark);

    int updateCheckinException(@Param("checkinId") Long checkinId,
                               @Param("operatorUserId") Long operatorUserId,
                               @Param("remark") String remark);

    int updateScheduleSignedCount(@Param("scheduleId") Long scheduleId,
                                  @Param("delta") Integer delta);

    int consumeMemberCard(@Param("cardId") Long cardId,
                          @Param("consumeTimes") Integer consumeTimes,
                          @Param("consumeAmount") BigDecimal consumeAmount);

    List<FitnessCoachAppVo.StudentVo> selectCoachStudentList(@Param("coachId") Long coachId,
                                                             @Param("keyword") String keyword);

    FitnessCoachAppVo.StudentVo selectCoachStudentDetail(@Param("coachId") Long coachId,
                                                         @Param("memberId") Long memberId);

    int updateCoachProfile(@Param("coachId") Long coachId,
                           @Param("bo") FitnessCoachAppBo.ProfileUpdateBo bo);
}

