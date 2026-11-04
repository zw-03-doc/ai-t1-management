package org.dromara.test;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.fitness.domain.dto.FitnessTrainingModels.*;
import org.dromara.fitness.domain.model.FitnessLoginUser.MemberLoginUser;
import org.dromara.fitness.mapper.FitnessTrainingMapper;
import org.dromara.fitness.service.impl.FitnessTrainingService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("training")
class FitnessTrainingLifecycleTest {
    private final FitnessTrainingMapper mapper = mock(FitnessTrainingMapper.class);
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    private final FitnessTrainingService service = new FitnessTrainingService(mapper, json);

    @Test
    void progressKeepsWorkoutOpenAndPersistsItsTargetPosition() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = member()) {
            Plan plan = plan("active");
            when(mapper.selectPlan("tenant-a", 10L, true)).thenReturn(plan);
            when(mapper.selectWorkout("tenant-a", 100L)).thenReturn(workout(100L, 1, "planned", today()));
            when(mapper.saveWorkoutProgress("tenant-a", 100L, today())).thenReturn(1);
            LogInput input = log(false);
            Workout saved = service.saveLog(100L, input);
            assertEquals("in_progress", saved.getStatus());
            assertEquals(0, saved.getSets().get(0).getTargetSetIndex());
            assertEquals(new BigDecimal("22.679619"), saved.getSets().get(0).getWeightKg());
            verify(mapper, never()).completeWorkout(anyString(), anyLong(), any());
            verify(mapper, never()).setPlanStatus(anyString(), anyLong(), anyString());
        }
    }

    @Test
    void finalCompletionClosesExhaustedPlanButLegacyClientsStillComplete() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = member()) {
            when(mapper.selectPlan("tenant-a", 10L, true)).thenReturn(plan("active"));
            when(mapper.selectWorkout("tenant-a", 100L)).thenReturn(workout(100L, 1, "in_progress", today()));
            when(mapper.completeWorkout("tenant-a", 100L, today())).thenReturn(1);
            when(mapper.countOpenWorkouts("tenant-a", 10L)).thenReturn(0);
            LogInput input = log(true);
            input.setFinish(null);
            assertEquals("completed", service.saveLog(100L, input).getStatus());
            verify(mapper).setPlanStatus("tenant-a", 10L, "completed");
        }
    }

    @Test
    void duplicateOrWrongTargetCannotErasePreviouslySavedGroups() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = member()) {
            when(mapper.selectPlan("tenant-a", 10L, true)).thenReturn(plan("active"));
            when(mapper.selectWorkout("tenant-a", 100L)).thenReturn(workout(100L, 1, "in_progress", today()));
            LogInput duplicate = log(false);
            duplicate.setSets(List.of(duplicate.getSets().get(0), duplicate.getSets().get(0)));
            assertThrows(ServiceException.class, () -> service.saveLog(100L, duplicate));
            LogInput outside = log(false); outside.getSets().get(0).setTargetSetIndex(1);
            assertThrows(ServiceException.class, () -> service.saveLog(100L, outside));
            verify(mapper, never()).deleteSets(anyString(), anyLong());
        }
    }

    @Test
    void extensionAppendsUniqueSequencesWithoutChangingOldInstances() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = member()) {
            when(mapper.selectPlan("tenant-a", 10L, true)).thenReturn(plan("completed"));
            Workout historical = workout(100L, 4, "completed", today().minusDays(5));
            when(mapper.selectWorkouts("tenant-a", 10L)).thenReturn(List.of(historical));
            StartInput input = new StartInput(); input.setStartDate(today()); input.setCycles(2);
            service.extend(10L, input);
            ArgumentCaptor<Workout> inserted = ArgumentCaptor.forClass(Workout.class);
            verify(mapper, times(2)).insertWorkout(inserted.capture());
            assertEquals(5, inserted.getAllValues().get(0).getSequenceNumber());
            assertEquals(today().plusDays(1), inserted.getAllValues().get(1).getPlannedDate());
            assertEquals(today().minusDays(5), historical.getPlannedDate());
            verify(mapper, never()).updateWorkoutDefinition(any());
            verify(mapper).setPlanStatus("tenant-a", 10L, "active");
        }
    }

    @Test
    void extensionCannotOverlapExistingCalendar() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = member()) {
            when(mapper.selectPlan("tenant-a", 10L, true)).thenReturn(plan("active"));
            when(mapper.selectWorkouts("tenant-a", 10L)).thenReturn(List.of(workout(100L, 1, "planned", today().plusDays(2))));
            StartInput input = new StartInput(); input.setStartDate(today()); input.setCycles(1);
            assertThrows(ServiceException.class, () -> service.extend(10L, input));
            verify(mapper, never()).insertWorkout(any());
        }
    }

    @Test
    void privateCopyCreatesOwnedDraftAndLocksTargetMember() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = member()) {
            Plan source = plan("archived");
            AtomicReference<Plan> inserted = new AtomicReference<>();
            when(mapper.selectPlan(eq("tenant-a"), anyLong(), eq(false))).thenAnswer(call ->
                call.<Long>getArgument(1).equals(10L) ? source : inserted.get());
            when(mapper.insertPlan(any())).thenAnswer(call -> { inserted.set(call.getArgument(0)); return 1; });
            Plan copy = service.copy(10L, new CopyInput());
            assertEquals("private", copy.getVisibility()); assertEquals("draft", copy.getStatus());
            assertEquals(7L, copy.getMemberId()); assertNotEquals(source.getPlanId(), copy.getPlanId());
            assertEquals("archived", source.getStatus());
            verify(mapper).lockActiveMember("tenant-a", 7L);
            verify(mapper, never()).insertWorkout(any());
        }
    }

    @Test
    void copyCannotAssignMembersPlanToAnotherMember() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = member()) {
            when(mapper.selectPlan("tenant-a", 10L, false)).thenReturn(plan("active"));
            CopyInput input = new CopyInput(); input.setMemberId(88L);
            assertThrows(ServiceException.class, () -> service.copy(10L, input));
            verify(mapper, never()).insertPlan(any());
        }
    }

    @Test
    void archiveStopsOnlyPendingSchedulesAndNeverDeletesActualHistory() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = member()) {
            Plan plan = plan("active");
            when(mapper.selectPlan(eq("tenant-a"), eq(10L), anyBoolean())).thenReturn(plan);
            when(mapper.selectWorkouts("tenant-a", 10L)).thenReturn(List.of(workout(100L, 1, "completed", today())));
            service.archive(10L);
            verify(mapper).cancelPendingWorkouts("tenant-a", 10L);
            verify(mapper).setPlanStatus("tenant-a", 10L, "archived");
            verify(mapper, never()).deleteSets(anyString(), anyLong());
        }
    }

    @Test
    void archiveAndDefinitionEditsCannotDiscardInProgressSnapshots() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = member()) {
            Plan plan = plan("active");
            when(mapper.selectPlan(eq("tenant-a"), eq(10L), anyBoolean())).thenReturn(plan);
            Workout progress = workout(100L, 1, "in_progress", today());
            when(mapper.selectWorkouts("tenant-a", 10L)).thenReturn(List.of(progress));
            assertThrows(ServiceException.class, () -> service.archive(10L));
            service.update(10L, FitnessTrainingRulesTest.plan());
            verify(mapper, never()).updateWorkoutDefinition(progress);
            verify(mapper, never()).cancelPendingWorkouts(anyString(), anyLong());
        }
    }

    @Test
    void previewFindsCollisionAndDifferentDateUnblocksWithoutMovingCompletedWorkouts() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = member()) {
            Plan plan = plan("active");
            when(mapper.selectPlan(eq("tenant-a"), eq(10L), anyBoolean())).thenReturn(plan);
            when(mapper.selectWorkouts("tenant-a", 10L)).thenReturn(List.of(
                workout(100L, 1, "planned", today().minusDays(2)), workout(101L, 2, "completed", today().minusDays(1))));
            ShiftInput input = new ShiftInput(); input.setFromDate(today().minusDays(2)); input.setDays(1);
            assertFalse(service.previewShift(10L, input).isCanApply());
            input.setDays(2);
            ShiftPreview valid = service.previewShift(10L, input);
            assertTrue(valid.isCanApply()); assertEquals(1, valid.getAffectedCount());
            assertEquals(today(), valid.getChanges().get(0).getAfterDate());
            input.setExpectedVersion(0);
            assertThrows(ServiceException.class, () -> service.shift(10L, input));
            verify(mapper, never()).shiftWorkouts(anyString(), anyLong(), any(), anyInt());
            input.setExpectedVersion(2); when(mapper.shiftWorkouts("tenant-a", 10L, input.getFromDate(), 2)).thenReturn(1);
            service.shift(10L, input);
            verify(mapper).shiftWorkouts("tenant-a", 10L, input.getFromDate(), 2);
        }
    }

    @Test
    void adminCanReadDisabledMembersArchiveButCannotWriteOrImpersonate() throws Exception {
        LoginUser admin = new LoginUser(); admin.setTenantId("tenant-a"); admin.setUserId(1L);
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getLoginUser).thenReturn(admin); login.when(LoginHelper::isTenantAdmin).thenReturn(true);
            MemberContext context = new MemberContext(); context.setMemberId(7L); context.setDisplayName("停用会员"); context.setMemberStatus("disabled");
            when(mapper.selectMemberContext("tenant-a", 7L)).thenReturn(context);
            assertFalse(service.context(7L).isCanEdit());
            assertEquals("admin", service.context(7L).getActorType());
            assertThrows(ServiceException.class, () -> service.create(FitnessTrainingRulesTest.plan()));
            assertThrows(ServiceException.class, () -> service.archive(10L));
            assertThrows(ServiceException.class, () -> service.context(99L));
            verify(mapper, never()).insertPlan(any());
        }
    }

    @Test
    void archivedPlanCannotReceiveMoreActualSets() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = member()) {
            when(mapper.selectPlan("tenant-a", 10L, true)).thenReturn(plan("archived"));
            when(mapper.selectWorkout("tenant-a", 100L)).thenReturn(workout(100L, 1, "completed", today()));
            assertThrows(ServiceException.class, () -> service.saveLog(100L, log(true)));
            verify(mapper, never()).deleteSets(anyString(), anyLong());
        }
    }

    private MockedStatic<LoginHelper> member() {
        MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class);
        MemberLoginUser member = new MemberLoginUser(); member.setTenantId("tenant-a"); member.setUserId(1007L); member.setMemberId(7L);
        login.when(LoginHelper::getLoginUser).thenReturn(member);
        when(mapper.countActiveMember("tenant-a", 7L)).thenReturn(1);
        when(mapper.lockActiveMember("tenant-a", 7L)).thenReturn(7L);
        return login;
    }
    private Plan plan(String status) throws Exception {
        Plan plan = new Plan(); plan.setTenantId("tenant-a"); plan.setPlanId(10L); plan.setMemberId(7L);
        plan.setName("私人训练"); plan.setVisibility("private"); plan.setStatus(status); plan.setCycleDays(1); plan.setVersion(2);
        plan.setStartDate(today().minusDays(6)); plan.setDaysJson(json.writeValueAsString(FitnessTrainingRulesTest.plan().getDays()));
        return plan;
    }
    private Workout workout(long id, int sequence, String status, LocalDate date) throws Exception {
        Workout workout = new Workout(); workout.setWorkoutId(id); workout.setPlanId(10L); workout.setTenantId("tenant-a");
        workout.setMemberId(7L); workout.setSequenceNumber(sequence); workout.setDayNumber(1); workout.setTitle("推");
        workout.setOriginalDate(date); workout.setPlannedDate(date); workout.setStatus(status);
        workout.setExercisesJson(json.writeValueAsString(FitnessTrainingRulesTest.plan().getDays().get(0).getExercises()));
        return workout;
    }
    private LogInput log(boolean finish) {
        LogInput input = new LogInput(); input.setPerformedDate(today()); input.setFinish(finish);
        SetInput set = new SetInput(); set.setExerciseKey("bench"); set.setExerciseName("卧推"); set.setTargetSetIndex(0);
        set.setSetType("working"); set.setReps(8); set.setWeight(new BigDecimal("50")); set.setUnit("lb");
        input.setSets(List.of(set)); return input;
    }
    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }
}
