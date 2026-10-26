package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.fitness.domain.vo.FitnessEngagementVo;
import java.util.Date;
import java.util.List;

public interface FitnessOperationsMapper {
    int insertBehavior(@Param("id") Long id, @Param("tenantId") String tenantId,
        @Param("actorId") Long actorId, @Param("actorType") String actorType,
        @Param("memberId") Long memberId, @Param("storeId") Long storeId,
        @Param("scheduleId") Long scheduleId, @Param("reservationId") Long reservationId,
        @Param("checkinId") Long checkinId, @Param("action") String action,
        @Param("source") String source, @Param("remark") String remark);
    Page<FitnessEngagementVo.Behavior> selectBehaviors(Page<FitnessEngagementVo.Behavior> page,
        @Param("tenantId") String tenantId, @Param("memberId") Long memberId,
        @Param("action") String action);
    int insertInterest(@Param("id") Long id, @Param("tenantId") String tenantId,
        @Param("memberId") Long memberId, @Param("courseId") Long courseId,
        @Param("coachId") Long coachId, @Param("sessionId") String sessionId,
        @Param("eventId") String eventId, @Param("seconds") int seconds, @Param("source") String source);
    int countInterestEvent(@Param("tenantId") String tenantId, @Param("memberId") Long memberId,
        @Param("eventId") String eventId);
    Date selectLastInterestTime(@Param("tenantId") String tenantId, @Param("memberId") Long memberId);
    Page<FitnessEngagementVo.Interest> selectInterests(Page<FitnessEngagementVo.Interest> page,
        @Param("tenantId") String tenantId, @Param("coachId") Long coachId,
        @Param("memberId") Long memberId);
    Long selectInterestCoach(@Param("tenantId") String tenantId, @Param("memberId") Long memberId,
        @Param("defaultCoachId") Long defaultCoachId, @Param("storeId") Long storeId);
    int countCoachConflicts(@Param("tenantId") String tenantId, @Param("coachId") Long coachId,
        @Param("scheduleId") Long scheduleId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    int transitionCheckin(@Param("checkinId") Long checkinId, @Param("tenantId") String tenantId,
        @Param("operatorId") Long operatorId, @Param("status") String status,
        @Param("consumeTimes") int consumeTimes, @Param("consumeAmount") java.math.BigDecimal consumeAmount,
        @Param("remark") String remark);
}
