package org.dromara.fitness.domain.bo;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Client sends foreground engagement only, never a user identity. */
public final class FitnessEngagementBo {
    private FitnessEngagementBo() { }

    @Data
    public static class Interest {
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+")
        private String sessionId;
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+")
        private String eventId;
        @NotNull @Min(1) @Max(60)
        private Integer activeSeconds;
        @NotBlank @Pattern(regexp = "web|mini")
        private String source;
    }

    @Data
    public static class ProxyCheckin {
        @NotNull
        private Long reservationId;
        @NotBlank @Size(max = 500)
        private String remark;
        @NotBlank @Pattern(regexp = "web|mini")
        private String source;
    }
}
