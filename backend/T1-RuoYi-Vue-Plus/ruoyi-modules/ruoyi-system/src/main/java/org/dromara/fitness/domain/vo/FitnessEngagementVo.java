package org.dromara.fitness.domain.vo;

import lombok.Data;
import java.util.Date;

public final class FitnessEngagementVo {
    private FitnessEngagementVo() { }

    @Data
    public static class Behavior {
        private Long behaviorId;
        private Long actorId;
        private String actorType;
        private String actorName;
        private Long memberId;
        private String memberName;
        private Long storeId;
        private String storeName;
        private Long scheduleId;
        private Long reservationId;
        private Long checkinId;
        private String action;
        private String source;
        private String remark;
        private Date createdAt;
    }

    @Data
    public static class Interest {
        private Long memberId;
        private String memberName;
        private Long courseId;
        private String courseName;
        private Long activeSeconds;
        private Long viewCount;
        private Date lastViewedAt;
    }
}
