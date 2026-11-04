package org.dromara.test.fitness;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.*;
import org.dromara.fitness.mapper.*;
import org.dromara.fitness.service.support.FitnessOperationsService;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class FitnessAttendanceFlowTest {
    final FitnessOperationsMapper ops=mock(FitnessOperationsMapper.class);
    final FitnessScheduleMapper schedules=mock(FitnessScheduleMapper.class);
    final FitnessReservationMapper reservations=mock(FitnessReservationMapper.class);
    final FitnessCheckinMapper checkins=mock(FitnessCheckinMapper.class);
    final FitnessCoachMapper coaches=mock(FitnessCoachMapper.class);
    final FitnessOperationsService service=spy(new FitnessOperationsService(ops,mock(FitnessCourseMapper.class),coaches,mock(FitnessStoreMapper.class),schedules,reservations,mock(FitnessMemberMapper.class),checkins));
    private FitCourseSchedule schedule() {
        FitCourseSchedule s=new FitCourseSchedule(); s.setScheduleId(1L); s.setTenantId("000000"); s.setStoreId(2L); s.setCoachId(3L); s.setScheduleStatus("open");
        s.setEndTime(new Date(System.currentTimeMillis()-60000)); s.setReservedCount(3); s.setSignedCount(3);
        when(schedules.selectById(1L)).thenReturn(s); return s;
    }
    private FitCourseReservation reservation() {
        var r=new FitCourseReservation(); r.setReservationId(5L); r.setScheduleId(1L); r.setMemberId(6L); r.setStoreId(2L); r.setTenantId("000000"); r.setReservationStatus("reserved");
        when(reservations.selectOne(any())).thenReturn(r); when(reservations.updateById(any(FitCourseReservation.class))).thenReturn(1);
        doNothing().when(service).audit(any(),any(),any(),any(),any(),anyString(),anyString(),any()); return r;
    }
    @Test void fullyHandledClassCanFinishWithoutErasingHistoricalCounts() {
        var s=schedule(); when(reservations.selectCount(any())).thenReturn(0L);
        assertDoesNotThrow(() -> service.requireScheduleCanFinish(s)); assertEquals(3,s.getReservedCount()); assertEquals(3,s.getSignedCount());
    }
    @Test void pendingOrFutureClassCannotFinish() {
        var s=schedule(); when(reservations.selectCount(any())).thenReturn(1L); assertThrows(ServiceException.class,()->service.requireScheduleCanFinish(s));
        when(reservations.selectCount(any())).thenReturn(0L); s.setEndTime(new Date(System.currentTimeMillis()+60000)); assertThrows(ServiceException.class,()->service.requireScheduleCanFinish(s));
    }
    @Test void absenceRetainsReservationAndWritesReasonToAudit() {
        try(MockedStatic<FitnessSecurityUtils> auth=mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000"); schedule(); var r=reservation();
            service.resolveReservation(5L,"absent","会员未到场",null);
            assertEquals("absent",r.getReservationStatus()); verify(service).audit(6L,2L,1L,5L,null,"admin_resolve_reservation","web","absent: 会员未到场");
            verify(schedules,never()).updateReservedCount(anyLong(),anyInt());
        }
    }
    @Test void cannotMarkAbsentBeforeEndOrDiscardPendingCheckin() {
        try(MockedStatic<FitnessSecurityUtils> auth=mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000"); var s=schedule(); reservation(); s.setEndTime(new Date(System.currentTimeMillis()+60000));
            assertThrows(ServiceException.class,()->service.resolveReservation(5L,"absent","未到场",null));
            var c=new FitCheckinRecord(); c.setCheckinStatus("success"); when(checkins.selectOne(any())).thenReturn(c);
            assertThrows(ServiceException.class,()->service.resolveReservation(5L,"leave","请假",null)); verify(reservations,never()).updateById(any(FitCourseReservation.class));
        }
    }
    @Test void reviewedExceptionCanBeResolvedAsLeaveWithoutDebit() {
        try(MockedStatic<FitnessSecurityUtils> auth=mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000"); schedule(); var r=reservation();
            var c=new FitCheckinRecord(); c.setCheckinId(7L); c.setCheckinStatus("reversed"); when(checkins.selectOne(any())).thenReturn(c);
            service.resolveReservation(5L,"leave","误签到且会员已请假",null); assertEquals("leave",r.getReservationStatus()); verifyNoInteractions(ops);
        }
    }
    @Test void leaveBeforeClassReleasesExactlyOneSeatAndCannotBeRepeated() {
        try(MockedStatic<FitnessSecurityUtils> auth=mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000"); var s=schedule(); var r=reservation();
            s.setStartTime(new Date(System.currentTimeMillis()+60000)); s.setEndTime(new Date(System.currentTimeMillis()+3600000));
            when(schedules.updateReservedCount(1L,-1)).thenReturn(1);
            service.resolveReservation(5L,"leave","提前请假",null);
            assertEquals("leave",r.getReservationStatus());
            assertThrows(ServiceException.class,()->service.resolveReservation(5L,"leave","重复提交",null));
            verify(schedules,times(1)).updateReservedCount(1L,-1);
            verify(service).audit(6L,2L,1L,5L,null,"admin_resolve_reservation","web","leave: 提前请假；开课前请假，已释放1个名额");
        }
    }
    @Test void restoringExceptionReusesRecordAndRequiresAuditReason() {
        try(MockedStatic<FitnessSecurityUtils> auth=mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000"); schedule(); reservation();
            var c=new FitCheckinRecord(); c.setCheckinId(7L); c.setReservationId(5L); c.setTenantId("000000"); when(checkins.selectById(7L)).thenReturn(c);
            when(ops.restoreCheckin(7L,"000000","核实已到场")).thenReturn(1);
            service.restoreCheckin(7L,"核实已到场",null);
            verify(service).audit(6L,2L,1L,5L,7L,"admin_restore_checkin","web","核实已到场"); verify(checkins,never()).insert(any(FitCheckinRecord.class));
            assertThrows(ServiceException.class,()->service.restoreCheckin(7L," ",null));
        }
    }
    @Test void restoreRejectsAnotherCoachResolvedReservationAndRepeatedTransition() {
        try(MockedStatic<FitnessSecurityUtils> auth=mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000"); schedule(); var r=reservation();
            var c=new FitCheckinRecord(); c.setCheckinId(7L); c.setReservationId(5L); when(checkins.selectById(7L)).thenReturn(c);
            assertThrows(ServiceException.class,()->service.restoreCheckin(7L,"核实",99L));
            assertThrows(ServiceException.class,()->service.restoreCheckin(7L,"核实",null));
            r.setReservationStatus("signed"); assertThrows(ServiceException.class,()->service.restoreCheckin(7L,"核实",null));
        }
    }
}
