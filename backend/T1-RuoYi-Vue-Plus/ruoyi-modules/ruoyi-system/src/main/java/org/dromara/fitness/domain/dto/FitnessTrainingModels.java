package org.dromara.fitness.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Training definitions are copied into each workout so edits cannot rewrite past training. */
public final class FitnessTrainingModels {
    private FitnessTrainingModels() { }

    @Data
    public static class TargetSet {
        private String setType = "working";
        private Integer reps;
        private BigDecimal weight;
        private String unit = "kg";
        private BigDecimal rpe;
    }

    @Data
    public static class Exercise {
        private String key;
        private String name;
        private String instructions;
        private List<String> imageUrls = new ArrayList<>();
        private String videoUrl;
        private List<TargetSet> targets = new ArrayList<>();
    }

    @Data
    public static class Day {
        private Integer dayNumber;
        private String title;
        private Boolean restDay = false;
        private List<Exercise> exercises = new ArrayList<>();
    }

    @Data
    public static class PlanInput {
        private Long memberId;
        private String name;
        private String description;
        private String visibility = "private";
        private Integer cycleDays;
        private List<Day> days = new ArrayList<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Plan extends PlanInput {
        private Long planId;
        private String templateId;
        private String status;
        private Long creatorId;
        private String creatorType;
        private LocalDate startDate;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private Integer version;
        @JsonIgnore private String tenantId;
        @JsonIgnore private String daysJson;
    }

    @Data
    public static class StartInput {
        private LocalDate startDate;
        private Integer cycles = 4;
    }

    @Data
    public static class ShiftInput {
        private LocalDate fromDate;
        private Integer days = 1;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SetInput extends TargetSet {
        private String exerciseKey;
        private String exerciseName;
    }

    @Data
    public static class LogInput {
        private LocalDate performedDate;
        private List<SetInput> sets = new ArrayList<>();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SetLog extends SetInput {
        private Long setId;
        private Long workoutId;
        private Integer setNumber;
        private BigDecimal weightKg;
        @JsonIgnore private String tenantId;
    }

    @Data
    public static class Workout {
        private Long workoutId;
        private Long planId;
        private Long memberId;
        private Integer sequenceNumber;
        private Integer dayNumber;
        private String title;
        private LocalDate originalDate;
        private LocalDate plannedDate;
        private LocalDate performedDate;
        private String status;
        private List<Exercise> exercises = new ArrayList<>();
        private List<SetLog> sets = new ArrayList<>();
        @JsonIgnore private String tenantId;
        @JsonIgnore private String exercisesJson;
    }

    @Data
    public static class StatsPoint {
        private Long workoutId;
        private LocalDate date;
        private String exerciseKey;
        private String exerciseName;
        private BigDecimal maxWorkingWeightKg;
        private BigDecimal workingVolumeKg;
        private Long workingSets;
        private Long warmupSets;
    }

    @Data
    public static class Stats {
        private String unit = "kg";
        private long completedWorkouts;
        private List<StatsPoint> points = new ArrayList<>();
    }

    @Data
    public static class Audit {
        private Long auditId;
        private Long planId;
        private Long memberId;
        private Long actorId;
        private String actorType;
        private String action;
        private String detail;
        private LocalDateTime createTime;
        @JsonIgnore private String tenantId;
    }

    @Data
    public static class Media {
        private String assetId;
        private Long planId;
        private String url;
        private String mediaType;
        private Long size;
        @JsonIgnore private String tenantId;
        @JsonIgnore private String fileName;
    }
}
