package org.dromara.test;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.dto.FitnessTrainingModels.*;
import org.dromara.fitness.service.impl.FitnessTrainingMediaService;
import org.dromara.fitness.service.support.FitnessTrainingRules;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("training")
class FitnessTrainingRulesTest {
    @Test
    void poundsRemainComparableToKilogramsWithoutFloatingPointDrift() {
        assertEquals(new BigDecimal("45.359237"), FitnessTrainingRules.toKg(new BigDecimal("100"), "lb"));
        assertEquals(new BigDecimal("20.500000"), FitnessTrainingRules.toKg(new BigDecimal("20.5"), "kg"));
        assertThrows(ServiceException.class, () -> FitnessTrainingRules.toKg(new BigDecimal("-1"), "kg"));
        assertThrows(ServiceException.class, () -> FitnessTrainingRules.toKg(BigDecimal.ONE, "stone"));
        assertThrows(ServiceException.class, () -> FitnessTrainingRules.toKg(new BigDecimal("0.00001"), "kg"));
    }

    @Test
    void restDaysAreRequiredToMakeTheCycleUnambiguous() {
        PlanInput input = plan();
        input.setCycleDays(2);
        assertThrows(ServiceException.class, () -> FitnessTrainingRules.validatePlan(input));
        Day rest = new Day();
        rest.setDayNumber(2);
        rest.setTitle("休息");
        rest.setRestDay(true);
        input.setDays(List.of(input.getDays().get(0), rest));
        assertDoesNotThrow(() -> FitnessTrainingRules.validatePlan(input));
        rest.setDayNumber(1);
        assertThrows(ServiceException.class, () -> FitnessTrainingRules.validatePlan(input));
    }

    @Test
    void plannedWeightCanBeBlankButActualWeightCannot() {
        TargetSet target = new TargetSet();
        target.setReps(10);
        target.setSetType("warmup");
        assertDoesNotThrow(() -> FitnessTrainingRules.validateSet(target, false));
        assertThrows(ServiceException.class, () -> FitnessTrainingRules.validateSet(target, true));
        target.setWeight(BigDecimal.ZERO);
        assertDoesNotThrow(() -> FitnessTrainingRules.validateSet(target, true));
        target.setSetType("hidden");
        assertThrows(ServiceException.class, () -> FitnessTrainingRules.validateSet(target, true));
    }

    @Test
    void shiftCrossesMonthAndYearWithCalendarDays() {
        assertEquals(LocalDate.of(2027, 1, 1), FitnessTrainingRules.shiftedDate(LocalDate.of(2026, 12, 31), 1));
        assertEquals(LocalDate.of(2028, 2, 29), FitnessTrainingRules.shiftedDate(LocalDate.of(2028, 2, 28), 1));
        assertThrows(ServiceException.class, () -> FitnessTrainingRules.shiftedDate(LocalDate.now(), -1));
        assertThrows(ServiceException.class, () -> FitnessTrainingRules.validateCycles(31, Integer.MAX_VALUE));
        assertEquals(364, FitnessTrainingRules.validateCycles(7, 52));
    }

    @Test
    void activeContentCannotPretendToBeTrainingMedia() {
        assertNull(FitnessTrainingMediaService.detectType("<svg><script>".getBytes(StandardCharsets.US_ASCII)));
        assertNull(FitnessTrainingMediaService.detectType("<!doctype html>".getBytes(StandardCharsets.US_ASCII)));
        assertEquals("image/jpeg", FitnessTrainingMediaService.detectType(new byte[]{(byte) 255, (byte) 216, (byte) 255}));
        assertEquals("video/mp4", FitnessTrainingMediaService.detectType(new byte[]{0, 0, 0, 24, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'}));
        PlanInput input = plan();
        input.getDays().get(0).getExercises().get(0).setVideoUrl("javascript:alert(1)");
        assertThrows(ServiceException.class, () -> FitnessTrainingRules.validatePlan(input));
    }

    static PlanInput plan() {
        PlanInput input = new PlanInput();
        input.setName("测试训练计划");
        input.setCycleDays(1);
        Day day = new Day();
        day.setDayNumber(1);
        day.setTitle("推");
        Exercise exercise = new Exercise();
        exercise.setKey("bench");
        exercise.setName("卧推");
        TargetSet target = new TargetSet();
        target.setReps(10);
        exercise.setTargets(List.of(target));
        day.setExercises(List.of(exercise));
        input.setDays(List.of(day));
        return input;
    }
}
