package org.dromara.test.fitness;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.*;
import org.dromara.fitness.domain.bo.FitnessEngagementBo;
import org.dromara.fitness.mapper.*;
import org.dromara.fitness.service.support.FitnessOperationsService;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class FitnessOperationsServiceTest {
    private final FitnessOperationsMapper ops = mock(FitnessOperationsMapper.class);
    private final FitnessCourseMapper courses = mock(FitnessCourseMapper.class);
    private final FitnessCoachMapper coaches = mock(FitnessCoachMapper.class);
    private final FitnessStoreMapper stores = mock(FitnessStoreMapper.class);
    private final FitnessScheduleMapper schedules = mock(FitnessScheduleMapper.class);
    private final FitnessReservationMapper reservations = mock(FitnessReservationMapper.class);
    private final FitnessMemberMapper members = mock(FitnessMemberMapper.class);
    private final FitnessCheckinMapper checkins = mock(FitnessCheckinMapper.class);
    private final FitnessOperationsService service = new FitnessOperationsService(ops,courses,coaches,stores,schedules,reservations,members,checkins);

    private FitCourseSchedule schedule() {
        FitCourseSchedule s = new FitCourseSchedule(); s.setScheduleId(1L); s.setTenantId("000000"); s.setCourseId(2L);
        s.setStoreId(5L); s.setCoachId(7L); s.setScheduleStatus("open"); s.setCapacity(1); s.setReservedCount(0);
        s.setStartTime(new Date(System.currentTimeMillis()+3_600_000));
        s.setEndTime(new Date(System.currentTimeMillis()+7_200_000));
        when(schedules.selectOne(any())).thenReturn(s); when(schedules.selectById(1L)).thenReturn(s);
        FitCourse c = new FitCourse(); c.setCourseId(2L); c.setTenantId("000000"); c.setStatus("0");
        when(courses.selectById(2L)).thenReturn(c);
        FitStore store = new FitStore(); store.setStoreId(5L); store.setTenantId("000000"); store.setStatus("0");
        when(stores.selectById(5L)).thenReturn(store);
        FitCoach coach = new FitCoach(); coach.setCoachId(7L); coach.setTenantId("000000"); coach.setStoreId(5L); coach.setHireStatus("0");
        when(coaches.selectOne(any())).thenReturn(coach);
        return s;
    }

    @Test void rejectsClosedFullOrUnassignedSessions() {
        try (MockedStatic<FitnessSecurityUtils> auth = mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000");
            FitCourseSchedule s = schedule(); s.setScheduleStatus("draft");
            assertThrows(ServiceException.class, () -> service.requireReservationWindow(1L, 4L));
            s.setScheduleStatus("open"); s.setReservedCount(1);
            assertThrows(ServiceException.class, () -> service.requireReservationWindow(1L, 4L));
            s.setReservedCount(0); when(schedules.selectScheduleStudentIds(1L)).thenReturn(List.of(9L));
            assertThrows(ServiceException.class, () -> service.requireReservationWindow(1L, 4L));
        }
    }

    @Test void anotherCoachCannotCheckInSomebodyElsesClass() {
        try (MockedStatic<FitnessSecurityUtils> auth = mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000");
            auth.when(FitnessSecurityUtils::getCurrentCoachId).thenReturn(8L);
            schedule(); FitCourseReservation r = new FitCourseReservation(); r.setReservationId(3L); r.setScheduleId(1L);
            when(reservations.selectOne(any())).thenReturn(r);
            FitnessEngagementBo.ProxyCheckin bo = new FitnessEngagementBo.ProxyCheckin(); bo.setReservationId(3L);
            assertThrows(ServiceException.class, () -> service.proxyCheckin(bo));
            verifyNoInteractions(checkins);
        }
    }

    @Test void confirmedOrReversedRecordCannotBeProcessedAgain() {
        try (MockedStatic<FitnessSecurityUtils> auth = mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000");
            when(ops.transitionCheckin(anyLong(),anyString(),anyLong(),anyString(),anyInt(),any(),any())).thenReturn(0);
            assertThrows(ServiceException.class, () -> service.confirmRecord(1L,2L,0,BigDecimal.ZERO,"确认"));
            assertThrows(ServiceException.class, () -> service.markException(1L,2L,"异常原因"));
        }
    }

    @Test void negativeConsumptionIsRejectedBeforeDatabaseMutation() {
        assertThrows(ServiceException.class, () -> service.confirmRecord(1L,2L,-1,BigDecimal.ZERO,""));
        assertThrows(ServiceException.class, () -> service.confirmRecord(1L,2L,0,new BigDecimal("-1"),""));
        verifyNoInteractions(ops);
    }

    @Test void existingCheckinPreventsCancellation() {
        when(checkins.selectCount(any())).thenReturn(1L);
        assertThrows(ServiceException.class, () -> service.requireNoCheckin(3L));
    }
}
