package org.dromara.fitness.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FitnessScheduleStatusBo(
    @NotBlank String targetStatus,
    @NotEmpty(message = "请先选择排课") @Size(max = 100, message = "一次最多可变更100个场次")
    List<@NotNull @Valid SelectedSchedule> rows
) {
    public record SelectedSchedule(@NotNull Long scheduleId, @NotBlank String expectedStatus) {}
}
