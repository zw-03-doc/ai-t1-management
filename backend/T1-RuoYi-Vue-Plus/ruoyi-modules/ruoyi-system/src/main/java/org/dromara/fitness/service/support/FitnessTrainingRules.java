package org.dromara.fitness.service.support;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.dto.FitnessTrainingModels.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/** Pure rules shared by writes and executable regression tests. */
public final class FitnessTrainingRules {
    public static final BigDecimal LB_TO_KG = new BigDecimal("0.45359237");
    private FitnessTrainingRules() { }

    public static BigDecimal toKg(BigDecimal weight, String unit) {
        require(weight != null && weight.signum() >= 0 && weight.compareTo(new BigDecimal("2000")) <= 0,
            "重量必须在 0 到 2000 之间");
        require(weight.stripTrailingZeros().scale() <= 4, "重量最多保留 4 位小数");
        require("kg".equals(unit) || "lb".equals(unit), "重量单位必须为 kg 或 lb");
        return ("lb".equals(unit) ? weight.multiply(LB_TO_KG) : weight).setScale(6, RoundingMode.HALF_UP);
    }

    public static void validatePlan(PlanInput input) {
        require(input != null, "计划不能为空");
        text(input.getName(), 100, true, "计划名称");
        text(input.getDescription(), 2000, false, "计划说明");
        require(Set.of("private", "public").contains(input.getVisibility() == null ? "" : input.getVisibility()), "公开范围无效");
        require(input.getCycleDays() != null && input.getCycleDays() >= 1 && input.getCycleDays() <= 31,
            "循环周期必须为 1 到 31 天（含休息日）");
        require(input.getDays() != null && input.getDays().size() == input.getCycleDays(), "请配置循环中的每一天，包含休息日");
        Set<Integer> dayNumbers = new HashSet<>();
        boolean hasTraining = false;
        for (Day day : input.getDays()) {
            require(day != null && day.getDayNumber() != null && day.getDayNumber() >= 1
                && day.getDayNumber() <= input.getCycleDays() && dayNumbers.add(day.getDayNumber()), "循环日编号不能重复或越界");
            text(day.getTitle(), 100, true, "训练日名称");
            require(day.getRestDay() != null, "请指定训练日或休息日");
            require(day.getExercises() != null && day.getExercises().size() <= 30, "每日最多 30 个动作");
            require(!day.getRestDay() || day.getExercises().isEmpty(), "休息日不能配置动作");
            require(day.getRestDay() || !day.getExercises().isEmpty(), "训练日请至少配置一个动作");
            hasTraining |= !day.getRestDay();
            Set<String> keys = new HashSet<>();
            for (Exercise exercise : day.getExercises()) {
                require(exercise != null, "动作不能为空");
                text(exercise.getKey(), 64, true, "动作标识");
                require(exercise.getKey().matches("[a-zA-Z0-9_-]+") && keys.add(exercise.getKey()), "动作标识须为不重复的字母数字或下划线");
                text(exercise.getName(), 100, true, "动作名称");
                text(exercise.getInstructions(), 4000, false, "动作要领");
                require(exercise.getImageUrls() != null && exercise.getImageUrls().size() <= 9, "每个动作最多 9 张图片");
                exercise.getImageUrls().forEach(FitnessTrainingRules::mediaUrl);
                if (exercise.getVideoUrl() != null && !exercise.getVideoUrl().isBlank()) mediaUrl(exercise.getVideoUrl());
                require(exercise.getTargets() != null && !exercise.getTargets().isEmpty() && exercise.getTargets().size() <= 30,
                    "每个动作请配置 1 到 30 组目标");
                exercise.getTargets().forEach(target -> validateSet(target, false));
            }
        }
        require(hasTraining, "计划至少包含一个训练日");
    }

    public static void validateSet(TargetSet set, boolean actual) {
        require(set != null, "训练组不能为空");
        require("warmup".equals(set.getSetType()) || "working".equals(set.getSetType()), "训练组类型必须为热身或正式组");
        require(set.getReps() != null && set.getReps() >= 1 && set.getReps() <= 1000, "次数须为 1 到 1000");
        require("kg".equals(set.getUnit()) || "lb".equals(set.getUnit()), "重量单位必须为 kg 或 lb");
        if (actual || set.getWeight() != null) toKg(set.getWeight(), set.getUnit());
        if (set.getRpe() != null) require(set.getRpe().compareTo(BigDecimal.ONE) >= 0
            && set.getRpe().compareTo(BigDecimal.TEN) <= 0, "主观强度 RPE 须为 1 到 10");
    }

    public static LocalDate shiftedDate(LocalDate date, int days) {
        require(date != null && days >= 1 && days <= 365, "顺延天数须为 1 到 365 天");
        return date.plusDays(days);
    }

    public static int validateCycles(Integer cycleDays, Integer cycles) {
        require(cycleDays != null && cycleDays >= 1 && cycles != null && cycles >= 1 && cycles <= 366
            && (long) cycleDays * cycles <= 366, "一次最多生成 366 天训练日程");
        return cycleDays * cycles;
    }

    public static void require(boolean valid, String message) {
        if (!valid) throw new ServiceException(message);
    }

    private static void text(String value, int max, boolean required, String label) {
        require(!required || value != null && !value.isBlank(), label + "不能为空");
        require(value == null || value.length() <= max, label + "过长");
    }

    private static void mediaUrl(String value) {
        require(value != null && value.length() <= 2048, "媒体地址无效");
        if (value.matches("/fitness/training/media/[0-9a-f-]{36}")) return;
        try {
            URI uri = URI.create(value);
            require("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null,
                "图片和短视频请使用 HTTPS 地址");
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("媒体地址无效");
        }
    }
}
