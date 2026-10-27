package org.dromara.fitness.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.model.LoginUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.fitness.domain.dto.FitnessTrainingModels.*;
import org.dromara.fitness.domain.model.FitnessLoginUser.CoachLoginUser;
import org.dromara.fitness.domain.model.FitnessLoginUser.MemberLoginUser;
import org.dromara.fitness.mapper.FitnessTrainingMapper;
import org.dromara.fitness.service.support.FitnessTrainingRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.dromara.fitness.service.support.FitnessTrainingRules.require;

@Service
@RequiredArgsConstructor
public class FitnessTrainingService {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private final FitnessTrainingMapper mapper;
    private final ObjectMapper objectMapper;

    private record Actor(String tenantId, Long actorId, String type, Long memberId, Long coachId) { }

    public List<Plan> templates() {
        Actor actor = actor();
        List<Plan> result = new ArrayList<>();
        result.add(defaultTemplate("three-split", "三分化 · 推 / 拉 / 腿", new String[][]{
            {"推", "bench:卧推", "press:肩上推举", "triceps:绳索下压"},
            {"拉", "pulldown:高位下拉", "row:坐姿划船", "curl:哑铃弯举"},
            {"腿", "squat:深蹲", "rdl:罗马尼亚硬拉", "calf:提踵"}, {"休息"}
        }));
        result.add(defaultTemplate("five-split", "五分化 · 胸 / 背 / 腿 / 肩 / 手臂", new String[][]{
            {"胸", "bench:卧推", "incline:上斜哑铃卧推", "fly:绳索夹胸"},
            {"背", "pulldown:高位下拉", "row:坐姿划船", "facepull:绳索面拉"},
            {"腿", "squat:深蹲", "rdl:罗马尼亚硬拉", "calf:提踵"},
            {"肩", "press:肩上推举", "lateral:哑铃侧平举", "rear:俯身飞鸟"},
            {"手臂", "curl:哑铃弯举", "triceps:绳索下压"}, {"休息"}, {"休息"}
        }));
        for (Plan stored : mapper.selectPublicPlans(actor.tenantId())) {
            Plan shared = new Plan();
            copyDefinition(hydrate(stored), shared);
            shared.setMemberId(null);
            shared.setTemplateId(String.valueOf(stored.getPlanId()));
            shared.setStatus("template");
            result.add(shared);
        }
        return result;
    }

    public List<Plan> plans(Long memberId) {
        Actor actor = actor();
        Long target = memberId;
        if ("member".equals(actor.type())) target = resolveMember(actor, memberId);
        else if ("coach".equals(actor.type()) || memberId != null) target = resolveMember(actor, memberId);
        return mapper.selectPlans(actor.tenantId(), target).stream().map(this::hydrate).toList();
    }

    public Plan detail(Long planId) {
        return accessiblePlan(actor(), planId, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public Plan create(PlanInput input) {
        FitnessTrainingRules.validatePlan(input);
        Actor actor = actor();
        Long memberId = resolveMember(actor, input.getMemberId());
        validateMediaReferences(actor, input, null);
        Plan plan = new Plan();
        copyDefinition(input, plan);
        plan.setMemberId(memberId);
        plan.setPlanId(IdWorker.getId());
        plan.setTenantId(actor.tenantId());
        plan.setCreatorId(actor.actorId());
        plan.setCreatorType(actor.type());
        plan.setStatus("draft");
        plan.setDaysJson(json(input.getDays()));
        mapper.insertPlan(plan);
        refreshMediaReferences(actor, plan);
        audit(actor, plan, "create", Map.of("name", plan.getName(), "visibility", plan.getVisibility()));
        return hydrate(mapper.selectPlan(actor.tenantId(), plan.getPlanId(), false));
    }

    @Transactional(rollbackFor = Exception.class)
    public Plan update(Long planId, PlanInput input) {
        FitnessTrainingRules.validatePlan(input);
        Actor actor = actor();
        Plan plan = accessiblePlan(actor, planId, true);
        validateMediaReferences(actor, input, planId);
        require(input.getMemberId() == null || input.getMemberId().equals(plan.getMemberId()), "不能转移计划所属会员");
        require(!"active".equals(plan.getStatus()) || Objects.equals(input.getCycleDays(), plan.getCycleDays()),
            "已启动计划不能修改循环天数，请复制为新计划");
        Map<String, Object> before = Map.of("name", plan.getName(), "visibility", plan.getVisibility(), "days", plan.getDays());
        copyDefinition(input, plan);
        plan.setDaysJson(json(input.getDays()));
        mapper.updatePlan(plan);
        if ("active".equals(plan.getStatus())) {
            Map<Integer, Day> days = input.getDays().stream().collect(Collectors.toMap(Day::getDayNumber, Function.identity()));
            for (Workout workout : mapper.selectWorkouts(actor.tenantId(), planId)) {
                if ("completed".equals(workout.getStatus())) continue;
                Day day = days.get(workout.getDayNumber());
                workout.setTitle(day.getTitle());
                workout.setExercisesJson(json(day.getExercises()));
                workout.setStatus(day.getRestDay() ? "rest" : "planned");
                mapper.updateWorkoutDefinition(workout);
            }
        }
        refreshMediaReferences(actor, plan);
        audit(actor, plan, "update", Map.of("before", before, "after", input));
        return hydrate(mapper.selectPlan(actor.tenantId(), planId, false));
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Workout> start(Long planId, StartInput input) {
        require(input != null, "启动参数不能为空");
        Actor actor = actor();
        Plan plan = accessiblePlan(actor, planId, true);
        require("draft".equals(plan.getStatus()), "计划已启动，请勿重复启动");
        LocalDate start = input.getStartDate() == null ? LocalDate.now(SHANGHAI) : input.getStartDate();
        require(!start.isBefore(LocalDate.now(SHANGHAI).minusDays(366)) && !start.isAfter(LocalDate.now(SHANGHAI).plusDays(366)),
            "启动日期须在前后一年内");
        int totalDays = FitnessTrainingRules.validateCycles(plan.getCycleDays(), input.getCycles());
        Map<Integer, Day> days = plan.getDays().stream().collect(Collectors.toMap(Day::getDayNumber, Function.identity()));
        for (int index = 0; index < totalDays; index++) {
            Day day = days.get(index % plan.getCycleDays() + 1);
            Workout workout = new Workout();
            workout.setWorkoutId(IdWorker.getId());
            workout.setTenantId(actor.tenantId());
            workout.setPlanId(planId);
            workout.setMemberId(plan.getMemberId());
            workout.setSequenceNumber(index + 1);
            workout.setDayNumber(day.getDayNumber());
            workout.setTitle(day.getTitle());
            workout.setOriginalDate(start.plusDays(index));
            workout.setPlannedDate(start.plusDays(index));
            workout.setStatus(day.getRestDay() ? "rest" : "planned");
            workout.setExercisesJson(json(day.getExercises()));
            mapper.insertWorkout(workout);
        }
        require(mapper.startPlan(actor.tenantId(), planId, start) == 1, "计划状态已变化，请刷新后重试");
        audit(actor, plan, "start", Map.of("startDate", start, "cycles", input.getCycles(), "days", totalDays));
        return loadWorkouts(actor, planId);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Workout> shift(Long planId, ShiftInput input) {
        require(input != null && input.getFromDate() != null && input.getDays() != null, "请指定起始日期和顺延天数");
        FitnessTrainingRules.shiftedDate(input.getFromDate(), input.getDays());
        Actor actor = actor();
        Plan plan = accessiblePlan(actor, planId, true);
        require("active".equals(plan.getStatus()), "请先启动计划");
        List<Workout> workouts = mapper.selectWorkouts(actor.tenantId(), planId);
        Set<LocalDate> completedDates = workouts.stream().filter(w -> "completed".equals(w.getStatus()))
            .map(Workout::getPlannedDate).collect(Collectors.toSet());
        for (Workout workout : workouts) {
            if (!"completed".equals(workout.getStatus()) && !workout.getPlannedDate().isBefore(input.getFromDate())) {
                require(!completedDates.contains(workout.getPlannedDate().plusDays(input.getDays())), "顺延后的日期已有完成训练，请选择其他天数");
            }
        }
        int changed = mapper.shiftWorkouts(actor.tenantId(), planId, input.getFromDate(), input.getDays());
        require(changed > 0, "该日期之后没有可顺延的训练日程");
        audit(actor, plan, "shift", Map.of("fromDate", input.getFromDate(), "days", input.getDays(), "changed", changed));
        return loadWorkouts(actor, planId);
    }

    public List<Workout> workouts(Long planId) {
        Actor actor = actor();
        accessiblePlan(actor, planId, false);
        return loadWorkouts(actor, planId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Workout saveLog(Long workoutId, LogInput input) {
        require(input != null && input.getPerformedDate() != null, "请填写实际训练日期");
        require(!input.getPerformedDate().isAfter(LocalDate.now(SHANGHAI)), "不能记录未来的实际训练");
        require(input.getSets() != null && !input.getSets().isEmpty() && input.getSets().size() <= 300, "请填写 1 到 300 组实际训练");
        Actor actor = actor();
        Workout initial = mapper.selectWorkout(actor.tenantId(), workoutId);
        require(initial != null, "训练日程不存在");
        // All writers lock the parent plan first, including corrections, edits and shifts.
        Plan plan = accessiblePlan(actor, initial.getPlanId(), true);
        Workout workout = mapper.selectWorkout(actor.tenantId(), workoutId);
        require(!"rest".equals(workout.getStatus()), "休息日不能提交训练记录，请先调整计划");
        require(plan.getStartDate() != null && !input.getPerformedDate().isBefore(plan.getStartDate()), "实际训练日期不能早于计划开始日期");
        Map<String, Exercise> exercises = parseList(workout.getExercisesJson(), Exercise.class).stream()
            .collect(Collectors.toMap(Exercise::getKey, Function.identity()));
        List<SetLog> oldSets = mapper.selectPlanSets(actor.tenantId(), plan.getPlanId()).stream()
            .filter(s -> s.getWorkoutId().equals(workoutId)).toList();
        List<SetLog> saved = new ArrayList<>();
        int number = 0;
        for (SetInput entry : input.getSets()) {
            FitnessTrainingRules.validateSet(entry, true);
            Exercise exercise = exercises.get(entry.getExerciseKey());
            require(exercise != null, "实际记录中包含不属于该训练日的动作");
            SetLog set = new SetLog();
            set.setSetId(IdWorker.getId());
            set.setTenantId(actor.tenantId());
            set.setWorkoutId(workoutId);
            set.setSetNumber(++number);
            set.setExerciseKey(exercise.getKey());
            set.setExerciseName(exercise.getName());
            set.setSetType(entry.getSetType());
            set.setReps(entry.getReps());
            set.setWeight(entry.getWeight());
            set.setUnit(entry.getUnit());
            set.setWeightKg(FitnessTrainingRules.toKg(entry.getWeight(), entry.getUnit()));
            set.setRpe(entry.getRpe());
            saved.add(set);
        }
        mapper.deleteSets(actor.tenantId(), workoutId);
        saved.forEach(mapper::insertSet);
        require(mapper.completeWorkout(actor.tenantId(), workoutId, input.getPerformedDate()) == 1, "训练状态已变化，请刷新重试");
        audit(actor, plan, "log", Map.of("workoutId", workoutId, "before", oldSets, "after", input));
        workout.setPerformedDate(input.getPerformedDate());
        workout.setStatus("completed");
        workout.setExercises(new ArrayList<>(exercises.values()));
        workout.setSets(saved);
        return workout;
    }

    public Stats stats(Long planId) {
        Actor actor = actor();
        accessiblePlan(actor, planId, false);
        Stats stats = new Stats();
        stats.setCompletedWorkouts(mapper.countCompleted(actor.tenantId(), planId));
        stats.setPoints(mapper.selectStats(actor.tenantId(), planId));
        return stats;
    }

    public List<Audit> audit(Long planId) {
        Actor actor = actor();
        require("admin".equals(actor.type()), "仅管理员可查看训练操作审计");
        return mapper.selectAudit(actor.tenantId(), planId);
    }

    public Plan mediaWriteAccess(Long planId) {
        return accessiblePlan(actor(), planId, false);
    }

    public Media mediaReadAccess(String assetId) {
        Actor actor = actor();
        return accessibleMedia(actor, assetId);
    }

    public void recordMediaUpload(Plan plan, Media media) {
        Actor actor = actor();
        resolveMember(actor, plan.getMemberId());
        audit(actor, plan, "upload", Map.of("assetId", media.getAssetId(), "size", media.getSize(), "type", media.getMediaType()));
    }

    private Media accessibleMedia(Actor actor, String assetId) {
        Media media = mapper.selectMedia(actor.tenantId(), assetId);
        require(media != null, "附件不存在或无权访问");
        Plan source = mapper.selectPlan(actor.tenantId(), media.getPlanId(), false);
        require(source != null, "附件所属计划不存在");
        if (!"public".equals(source.getVisibility()) && !canAccessMember(actor, source.getMemberId())) {
            boolean copiedAccess = mapper.selectMediaReferencePlans(actor.tenantId(), assetId).stream()
                .anyMatch(plan -> "public".equals(plan.getVisibility()) || canAccessMember(actor, plan.getMemberId()));
            require(copiedAccess, "无权访问该训练附件");
        }
        return media;
    }

    private void validateMediaReferences(Actor actor, PlanInput input, Long targetPlanId) {
        for (Day day : input.getDays()) for (Exercise exercise : day.getExercises()) {
            List<String> urls = new ArrayList<>(exercise.getImageUrls());
            if (exercise.getVideoUrl() != null && !exercise.getVideoUrl().isBlank()) urls.add(exercise.getVideoUrl());
            for (String url : urls) if (url.startsWith("/fitness/training/media/")) {
                Media media = accessibleMedia(actor, url.substring("/fitness/training/media/".length()));
                if ("public".equals(input.getVisibility()) && !media.getPlanId().equals(targetPlanId)) {
                    Plan source = mapper.selectPlan(actor.tenantId(), media.getPlanId(), false);
                    boolean publicCopy = mapper.selectMediaReferencePlans(actor.tenantId(), media.getAssetId()).stream()
                        .anyMatch(plan -> "public".equals(plan.getVisibility()) || plan.getPlanId().equals(targetPlanId));
                    require("public".equals(source.getVisibility()) || publicCopy, "公开计划不能引用其他私有计划的附件，请先复制上传");
                }
            }
        }
    }

    private boolean canAccessMember(Actor actor, Long memberId) {
        if ("admin".equals(actor.type())) return true;
        if ("member".equals(actor.type())) return actor.memberId().equals(memberId);
        return mapper.countCoachMember(actor.tenantId(), actor.coachId(), memberId) > 0;
    }

    private void refreshMediaReferences(Actor actor, Plan plan) {
        Set<String> ids = new HashSet<>();
        for (Day day : plan.getDays()) collectMediaIds(day.getExercises(), ids);
        // Completed snapshots retain their assets even when future exercise definitions change.
        for (Workout workout : mapper.selectWorkouts(actor.tenantId(), plan.getPlanId())) {
            if ("completed".equals(workout.getStatus())) collectMediaIds(parseList(workout.getExercisesJson(), Exercise.class), ids);
        }
        mapper.deleteMediaReferences(actor.tenantId(), plan.getPlanId());
        ids.forEach(id -> mapper.insertMediaReference(actor.tenantId(), plan.getPlanId(), id));
    }

    private void collectMediaIds(List<Exercise> exercises, Set<String> ids) {
        for (Exercise exercise : exercises) {
            List<String> urls = new ArrayList<>(exercise.getImageUrls());
            if (exercise.getVideoUrl() != null) urls.add(exercise.getVideoUrl());
            for (String url : urls) if (url.startsWith("/fitness/training/media/")) {
                ids.add(url.substring("/fitness/training/media/".length()));
            }
        }
    }

    private Actor actor() {
        StpUtil.checkLogin();
        LoginUser login = LoginHelper.getLoginUser();
        require(login != null && login.getTenantId() != null && !login.getTenantId().isBlank(), "登录租户无效");
        String tenant = login.getTenantId();
        if (login instanceof MemberLoginUser member) {
            require(mapper.countActiveMember(tenant, member.getMemberId()) == 1, "会员已停用或不存在");
            return new Actor(tenant, member.getUserId(), "member", member.getMemberId(), null);
        }
        if (login instanceof CoachLoginUser coach) {
            Long coachId = mapper.selectCoachId(tenant, coach.getUserId());
            require(coachId != null && coachId.equals(coach.getCoachId()), "教练已停用或不存在");
            return new Actor(tenant, coach.getUserId(), "coach", null, coachId);
        }
        if (LoginHelper.isSuperAdmin() || LoginHelper.isTenantAdmin() || StpUtil.hasPermission("fitness:training:manage")) {
            return new Actor(tenant, login.getUserId(), "admin", null, null);
        }
        Long coachId = mapper.selectCoachId(tenant, login.getUserId());
        require(coachId != null, "当前账户无训练计划权限");
        return new Actor(tenant, login.getUserId(), "coach", null, coachId);
    }

    private Long resolveMember(Actor actor, Long requested) {
        Long memberId = requested;
        if ("member".equals(actor.type())) {
            require(memberId == null || memberId.equals(actor.memberId()), "无权操作其他会员的训练计划");
            memberId = actor.memberId();
        }
        require(memberId != null && mapper.countActiveMember(actor.tenantId(), memberId) == 1, "请选择有效会员");
        if ("coach".equals(actor.type())) {
            require(mapper.countCoachMember(actor.tenantId(), actor.coachId(), memberId) > 0,
                "仅可操作本门店有效绑定会员的训练计划");
        }
        return memberId;
    }

    private Plan accessiblePlan(Actor actor, Long id, boolean lock) {
        Plan plan = mapper.selectPlan(actor.tenantId(), id, lock);
        require(plan != null, "计划不存在或无权访问");
        resolveMember(actor, plan.getMemberId());
        return hydrate(plan);
    }

    private Plan hydrate(Plan plan) {
        plan.setDays(parseList(plan.getDaysJson(), Day.class));
        return plan;
    }

    private List<Workout> loadWorkouts(Actor actor, Long planId) {
        List<Workout> workouts = mapper.selectWorkouts(actor.tenantId(), planId);
        Map<Long, List<SetLog>> grouped = mapper.selectPlanSets(actor.tenantId(), planId).stream()
            .collect(Collectors.groupingBy(SetLog::getWorkoutId));
        for (Workout workout : workouts) {
            workout.setExercises(parseList(workout.getExercisesJson(), Exercise.class));
            workout.setSets(grouped.getOrDefault(workout.getWorkoutId(), List.of()));
        }
        return workouts;
    }

    private void audit(Actor actor, Plan plan, String action, Object detail) {
        Audit audit = new Audit();
        audit.setAuditId(IdWorker.getId());
        audit.setTenantId(actor.tenantId());
        audit.setPlanId(plan.getPlanId());
        audit.setMemberId(plan.getMemberId());
        audit.setActorId(actor.actorId());
        audit.setActorType(actor.type());
        audit.setAction(action);
        audit.setDetail(json(detail));
        mapper.insertAudit(audit);
    }

    private void copyDefinition(PlanInput from, Plan to) {
        to.setName(from.getName());
        to.setDescription(from.getDescription());
        to.setVisibility(from.getVisibility());
        to.setCycleDays(from.getCycleDays());
        to.setDays(from.getDays());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize training data", ex);
        }
    }

    private <T> List<T> parseList(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to read training definition", ex);
        }
    }

    private Plan defaultTemplate(String id, String name, String[][] definition) {
        Plan plan = new Plan();
        plan.setTemplateId(id);
        plan.setName(name);
        plan.setDescription("免费基础模板，可按自己的训练经验修改动作、目标重量和休息日。实际重量由每次训练记录。");
        plan.setVisibility("public");
        plan.setStatus("template");
        plan.setCycleDays(definition.length);
        for (int index = 0; index < definition.length; index++) {
            String[] row = definition[index];
            Day day = new Day();
            day.setDayNumber(index + 1);
            day.setTitle(row[0]);
            day.setRestDay(row.length == 1);
            for (int e = 1; e < row.length; e++) {
                String[] parts = row[e].split(":", 2);
                Exercise exercise = new Exercise();
                exercise.setKey(parts[0]);
                exercise.setName(parts[1]);
                exercise.setInstructions("请在可控制的动作范围内完成，按实际训练填写重量；可补充自己的图片或短视频要领。");
                for (int set = 0; set < 4; set++) {
                    TargetSet target = new TargetSet();
                    target.setSetType(set == 0 ? "warmup" : "working");
                    target.setReps(set == 0 ? 12 : 10);
                    exercise.getTargets().add(target);
                }
                day.getExercises().add(exercise);
            }
            plan.getDays().add(day);
        }
        return plan;
    }
}
