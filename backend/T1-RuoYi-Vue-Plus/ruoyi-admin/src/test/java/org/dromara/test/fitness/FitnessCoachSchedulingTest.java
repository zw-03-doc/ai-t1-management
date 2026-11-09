package org.dromara.test.fitness;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.bo.FitnessCoachAppBo.ScheduleCreateBo;
import org.dromara.fitness.domain.bo.FitnessScheduleBo;
import org.dromara.fitness.domain.model.FitnessLoginUser.CoachLoginUser;
import org.dromara.fitness.domain.vo.FitnessCoachAppVo.AssignedCourseVo;
import org.dromara.fitness.domain.vo.FitnessCoachAppVo.ScheduleVo;
import org.dromara.fitness.mapper.FitnessCoachAppMapper;
import org.dromara.fitness.service.IFitnessScheduleService;
import org.dromara.fitness.service.impl.FitnessCoachAppServiceImpl;
import org.dromara.fitness.service.support.FitnessOperationsService;
import org.dromara.fitness.utils.FitnessBookingPolicy;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class FitnessCoachSchedulingTest {
    @Mock private FitnessCoachAppMapper mapper;
    @Mock private IFitnessScheduleService schedules;
    @Mock private FitnessOperationsService operations;
    @InjectMocks private FitnessCoachAppServiceImpl service;
    private MockedStatic<FitnessSecurityUtils> login;
    private AutoCloseable mocks;

    private final LocalDate day = LocalDate.now(FitnessBookingPolicy.ZONE).plusDays(10);
    private final ScheduleCreateBo request = new ScheduleCreateBo(42L, day, LocalTime.of(9, 30));
    private final AssignedCourseVo course = new AssignedCourseVo();

    @BeforeEach void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        CoachLoginUser coach = new CoachLoginUser();
        coach.setTenantId("000000");
        coach.setUserId(6L);
        coach.setCoachId(8L);
        login = mockStatic(FitnessSecurityUtils.class);
        login.when(FitnessSecurityUtils::getCoachLoginUser).thenReturn(coach);
        login.when(FitnessSecurityUtils::getCurrentCoachId).thenReturn(8L);
        login.when(FitnessSecurityUtils::getCurrentSource).thenReturn("mini");
        course.setCourseId(42L);
        course.setCourseName("力量训练");
        course.setDurationMinutes(45);
        course.setBookingAdvanceDays(7);
        course.setBookingCloseMinutes(30);
        when(mapper.lockAssignedCourse("000000", 8L, 6L, 42L)).thenReturn(course);
    }

    @AfterEach void close() throws Exception {
        login.close();
        mocks.close();
    }

    @Test void choicesAndWritesUseAuthenticatedAccountAndCurrentCourseAssignment() {
        when(mapper.selectAssignedCourses("000000", 8L, 6L)).thenReturn(List.of(course));
        assertEquals(List.of(course), service.queryAssignedCourses());
        when(mapper.lockAssignedCourse("000000", 8L, 6L, 42L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.createSchedule(request));
        verifyNoInteractions(schedules, operations);
    }

    @Test void inheritsCourseRulesAndAuditsOnlyAfterAnOpenSlotIsSaved() {
        when(schedules.insertByBo(any())).thenAnswer(call -> {
            FitnessScheduleBo slot = call.getArgument(0);
            assertEquals(8L, slot.getCoachId());
            assertEquals(42L, slot.getCourseId());
            assertEquals("open", slot.getScheduleStatus());
            assertEquals("normal", slot.getScheduleMode());
            assertNull(slot.getCapacity());
            assertNull(slot.getDisplayPrice());
            var start = day.atTime(9, 30).atZone(FitnessBookingPolicy.ZONE);
            assertEquals(start.toInstant(), slot.getStartTime().toInstant());
            assertEquals(start.plusMinutes(45).toInstant(), slot.getEndTime().toInstant());
            assertEquals(start.minusMinutes(30).toInstant(), slot.getReservationDeadline().toInstant());
            assertEquals(day.minusDays(7).atStartOfDay(FitnessBookingPolicy.ZONE).toInstant(), slot.getReservationOpenTime().toInstant());
            slot.setStoreId(5L);
            slot.setScheduleId(99L);
            return true;
        });
        ScheduleVo detail = new ScheduleVo();
        when(mapper.selectCoachScheduleDetail(8L, 99L)).thenReturn(detail);
        assertSame(detail, service.createSchedule(request));
        verify(operations).audit(null, 5L, 99L, null, null, "coach_create_schedule", "mini", "力量训练");
    }

    @Test void rejectsUnbookableDatesBeforeWriting() {
        var past = new ScheduleCreateBo(42L, day.minusDays(11), LocalTime.NOON);
        assertThrows(ServiceException.class, () -> service.createSchedule(past));
        course.setListingUntil(Date.from(day.minusDays(7).atStartOfDay(FitnessBookingPolicy.ZONE).toInstant()));
        assertThrows(ServiceException.class, () -> service.createSchedule(request));
        verifyNoInteractions(schedules, operations);
    }

    @Test void schedulingFailureNeverCreatesAnAuditEntry() {
        when(schedules.insertByBo(any())).thenThrow(new ServiceException("该教练在此时间段已有排课"));
        assertThrows(ServiceException.class, () -> service.createSchedule(request));
        verifyNoInteractions(operations);
    }
}
