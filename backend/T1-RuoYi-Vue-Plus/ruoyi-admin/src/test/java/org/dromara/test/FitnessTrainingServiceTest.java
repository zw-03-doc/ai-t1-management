package org.dromara.test;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.fitness.domain.dto.FitnessTrainingModels.*;
import org.dromara.fitness.domain.model.FitnessLoginUser.CoachLoginUser;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("training")
class FitnessTrainingServiceTest {
    private final FitnessTrainingMapper mapper = mock(FitnessTrainingMapper.class);
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    private final FitnessTrainingService service = new FitnessTrainingService(mapper, json);

    @Test
    void memberCannotReadOtherMembersEvenWhenPlanIsPublic() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = memberLogin()) {
            Plan foreign = storedPlan();
            foreign.setMemberId(88L);
            foreign.setVisibility("public");
            when(mapper.selectPlan("tenant-a", 10L, false)).thenReturn(foreign);
            assertThrows(ServiceException.class, () -> service.detail(10L));
            assertThrows(ServiceException.class, () -> service.stats(10L));
            verify(mapper, times(2)).selectPlan("tenant-a", 10L, false);
            verify(mapper, never()).selectWorkouts(anyString(), anyLong());
        }
    }

    @Test
    void coachMustHaveCurrentSameStoreBinding() throws Exception {
        CoachLoginUser coach = new CoachLoginUser();
        coach.setTenantId("tenant-a");
        coach.setUserId(20L);
        coach.setCoachId(30L);
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getLoginUser).thenReturn(coach);
            when(mapper.selectCoachId("tenant-a", 20L)).thenReturn(30L);
            when(mapper.countActiveMember("tenant-a", 7L)).thenReturn(1);
            when(mapper.selectPlan("tenant-a", 10L, false)).thenReturn(storedPlan());
            assertThrows(ServiceException.class, () -> service.detail(10L));
            when(mapper.countCoachMember("tenant-a", 30L, 7L)).thenReturn(1);
            assertEquals(7L, service.detail(10L).getMemberId());
        }
    }

    @Test
    void startLocksPlanAndCreatesDistinctCalendarInstances() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = memberLogin()) {
            when(mapper.selectPlan("tenant-a", 10L, true)).thenReturn(storedPlan());
            when(mapper.startPlan(eq("tenant-a"), eq(10L), any())).thenReturn(1);
            StartInput input = new StartInput();
            input.setStartDate(today());
            input.setCycles(3);
            service.start(10L, input);
            ArgumentCaptor<Workout> workouts = ArgumentCaptor.forClass(Workout.class);
            verify(mapper, times(3)).insertWorkout(workouts.capture());
            assertEquals(today().plusDays(2), workouts.getAllValues().get(2).getPlannedDate());
            assertEquals(3, workouts.getAllValues().get(2).getSequenceNumber());
            assertEquals(3, workouts.getAllValues().stream().map(Workout::getWorkoutId).distinct().count());
            verify(mapper).insertAudit(any());
        }
    }

    @Test
    void updatesPreserveCompletedWorkoutSnapshots() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = memberLogin()) {
            Plan stored = storedPlan();
            stored.setStatus("active");
            when(mapper.selectPlan("tenant-a", 10L, true)).thenReturn(stored);
            when(mapper.selectPlan("tenant-a", 10L, false)).thenReturn(stored);
            Workout done = workout(100L, "completed");
            Workout pending = workout(101L, "planned");
            when(mapper.selectWorkouts("tenant-a", 10L)).thenReturn(List.of(done, pending));
            service.update(10L, FitnessTrainingRulesTest.plan());
            verify(mapper).updateWorkoutDefinition(pending);
            verify(mapper, never()).updateWorkoutDefinition(done);
        }
    }

    @Test
    void invalidExerciseCannotReplaceExistingLogsAndValidPoundsAreNormalized() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = memberLogin()) {
            Plan stored = storedPlan();
            stored.setStatus("active");
            stored.setStartDate(today().minusDays(3));
            when(mapper.selectPlan("tenant-a", 10L, true)).thenReturn(stored);
            Workout workout = workout(100L, "planned");
            when(mapper.selectWorkout("tenant-a", 100L)).thenReturn(workout);
            when(mapper.completeWorkout("tenant-a", 100L, today())).thenReturn(1);
            LogInput input = new LogInput();
            input.setPerformedDate(today());
            SetInput set = new SetInput();
            set.setExerciseKey("unrelated");
            set.setReps(10);
            set.setWeight(new BigDecimal("100"));
            set.setUnit("lb");
            input.setSets(List.of(set));
            assertThrows(ServiceException.class, () -> service.saveLog(100L, input));
            verify(mapper, never()).deleteSets(anyString(), anyLong());
            set.setExerciseKey("bench");
            Workout saved = service.saveLog(100L, input);
            assertEquals(new BigDecimal("45.359237"), saved.getSets().get(0).getWeightKg());
            assertEquals("卧推", saved.getSets().get(0).getExerciseName());
            assertEquals("completed", saved.getStatus());
        }
    }

    @Test
    void mediaAccessNeverLooksOutsideAuthenticatedTenant() {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = memberLogin()) {
            assertThrows(ServiceException.class, () -> service.mediaReadAccess("asset-in-another-tenant"));
            verify(mapper).selectMedia("tenant-a", "asset-in-another-tenant");
        }
    }

    @Test
    void copiedTemplateMediaRemainsReadableAfterSourceBecomesPrivate() throws Exception {
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class); MockedStatic<LoginHelper> login = memberLogin()) {
            Media media = new Media();
            media.setAssetId("copied-asset");
            media.setPlanId(20L);
            Plan source = storedPlan();
            source.setPlanId(20L);
            source.setMemberId(88L);
            source.setVisibility("private");
            when(mapper.selectMedia("tenant-a", "copied-asset")).thenReturn(media);
            when(mapper.selectPlan("tenant-a", 20L, false)).thenReturn(source);
            assertThrows(ServiceException.class, () -> service.mediaReadAccess("copied-asset"));
            when(mapper.selectMediaReferencePlans("tenant-a", "copied-asset")).thenReturn(List.of(storedPlan()));
            assertEquals(media, service.mediaReadAccess("copied-asset"));
            // A media reference never grants access to the original author's records.
            assertThrows(ServiceException.class, () -> service.stats(20L));
        }
    }

    private MockedStatic<LoginHelper> memberLogin() {
        MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class);
        MemberLoginUser member = new MemberLoginUser();
        member.setTenantId("tenant-a");
        member.setUserId(1007L);
        member.setMemberId(7L);
        login.when(LoginHelper::getLoginUser).thenReturn(member);
        when(mapper.countActiveMember("tenant-a", 7L)).thenReturn(1);
        return login;
    }

    private Plan storedPlan() throws Exception {
        Plan plan = new Plan();
        plan.setTenantId("tenant-a");
        plan.setPlanId(10L);
        plan.setMemberId(7L);
        plan.setName("原计划");
        plan.setVisibility("private");
        plan.setStatus("draft");
        plan.setCycleDays(1);
        plan.setDaysJson(json.writeValueAsString(FitnessTrainingRulesTest.plan().getDays()));
        return plan;
    }

    private Workout workout(Long id, String status) throws Exception {
        Workout workout = new Workout();
        workout.setWorkoutId(id);
        workout.setTenantId("tenant-a");
        workout.setPlanId(10L);
        workout.setMemberId(7L);
        workout.setDayNumber(1);
        workout.setStatus(status);
        workout.setPlannedDate(today());
        workout.setExercisesJson(json.writeValueAsString(FitnessTrainingRulesTest.plan().getDays().get(0).getExercises()));
        return workout;
    }

    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }
}
