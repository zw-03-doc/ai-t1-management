package org.dromara.fitness.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.fitness.domain.dto.FitnessTrainingModels.*;
import java.time.LocalDate;
import java.util.List;

/** All statements have explicit tenant predicates, including checks and joins. */
public interface FitnessTrainingMapper {
    Long selectCoachId(@Param("tenantId") String tenantId, @Param("userId") Long userId);
    int countActiveMember(@Param("tenantId") String tenantId, @Param("memberId") Long memberId);
    int countCoachMember(@Param("tenantId") String tenantId, @Param("coachId") Long coachId, @Param("memberId") Long memberId);
    List<Plan> selectPlans(@Param("tenantId") String tenantId, @Param("memberId") Long memberId);
    List<Plan> selectPublicPlans(@Param("tenantId") String tenantId);
    Plan selectPlan(@Param("tenantId") String tenantId, @Param("planId") Long planId, @Param("lock") boolean lock);
    int insertPlan(@Param("plan") Plan plan);
    int updatePlan(@Param("plan") Plan plan);
    int startPlan(@Param("tenantId") String tenantId, @Param("planId") Long planId, @Param("startDate") LocalDate startDate);
    int insertWorkout(@Param("workout") Workout workout);
    List<Workout> selectWorkouts(@Param("tenantId") String tenantId, @Param("planId") Long planId);
    Workout selectWorkout(@Param("tenantId") String tenantId, @Param("workoutId") Long workoutId);
    int updateWorkoutDefinition(@Param("workout") Workout workout);
    int shiftWorkouts(@Param("tenantId") String tenantId, @Param("planId") Long planId,
                      @Param("fromDate") LocalDate fromDate, @Param("days") int days);
    int completeWorkout(@Param("tenantId") String tenantId, @Param("workoutId") Long workoutId,
                        @Param("performedDate") LocalDate performedDate);
    int deleteSets(@Param("tenantId") String tenantId, @Param("workoutId") Long workoutId);
    int insertSet(@Param("set") SetLog set);
    List<SetLog> selectPlanSets(@Param("tenantId") String tenantId, @Param("planId") Long planId);
    List<StatsPoint> selectStats(@Param("tenantId") String tenantId, @Param("planId") Long planId);
    long countCompleted(@Param("tenantId") String tenantId, @Param("planId") Long planId);
    int insertAudit(@Param("audit") Audit audit);
    List<Audit> selectAudit(@Param("tenantId") String tenantId, @Param("planId") Long planId);
    int insertMedia(@Param("media") Media media);
    Media selectMedia(@Param("tenantId") String tenantId, @Param("assetId") String assetId);
    List<Plan> selectMediaReferencePlans(@Param("tenantId") String tenantId, @Param("assetId") String assetId);
    int deleteMediaReferences(@Param("tenantId") String tenantId, @Param("planId") Long planId);
    int insertMediaReference(@Param("tenantId") String tenantId, @Param("planId") Long planId, @Param("assetId") String assetId);
}
