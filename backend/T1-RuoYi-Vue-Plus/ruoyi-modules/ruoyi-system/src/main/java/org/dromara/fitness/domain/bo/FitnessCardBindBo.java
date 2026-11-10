package org.dromara.fitness.domain.bo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FitnessCardBindBo(@NotNull Long memberId, @NotNull Long planId, Long storeId, Long bindCoachId,
                               @NotNull UUID requestId, @Size(max = 300) String remark) {}
