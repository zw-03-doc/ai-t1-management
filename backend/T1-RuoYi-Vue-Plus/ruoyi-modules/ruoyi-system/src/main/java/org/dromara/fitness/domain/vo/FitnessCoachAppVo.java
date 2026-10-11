package org.dromara.fitness.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 教练 App 视图对象集合
 *
 * @author CodeBuddy
 */
public final class FitnessCoachAppVo {

    private FitnessCoachAppVo() {
    }

    @Data
    public static class LoginVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String accessToken;

        private Long expireIn;

        private String clientId;

        private Boolean activated;

        private Boolean needActivation;

        private String openid;

        private String message;

        private CoachProfileVo profile;
    }


    @Data
    public static class HomeOverviewVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private HomeStatsVo stats;

        private List<ScheduleVo> todayScheduleList;

        private List<CheckinVo> pendingList;

        private List<MessageVo> messageList;
    }

    @Data
    public static class HomeStatsVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long todayScheduleCount;

        private Long pendingCheckinCount;

        private Long confirmedCount;

        private Long totalStudentCount;

        private Long weekSessionCount;
    }

    @Data
    public static class CoachProfileVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String tenantId;

        private Long userId;

        private String userName;

        private String nickName;

        private String phonenumber;

        private Long coachId;

        private Long storeId;

        private String storeName;

        private String coachName;

        private String mobile;

        private String avatarUrl;

        private String skillTags;

        private String levelCode;

        private String intro;

        private String hireStatus;

        private String userStatus;

        private String password;

        public String getDisplayName() {
            if (coachName != null && !coachName.isBlank()) {
                return coachName;
            }
            if (nickName != null && !nickName.isBlank()) {
                return nickName;
            }
            return userName;
        }
    }

    @Data
    public static class ScheduleVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long scheduleId;

        private Long storeId;

        private String storeName;

        private Long courseId;

        private String courseName;

        private String courseType;

        private Long coachId;

        private String coachName;

        private Date scheduleDate;

        private Date startTime;

        private Date endTime;

        private Integer capacity;

        private Integer reservedCount;

        private Integer signedCount;

        private String scheduleStatus;
    }

    @Data
    public static class CheckinVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long checkinId;

        private Long reservationId;

        private Long scheduleId;

        private Long storeId;

        private String storeName;

        private Long courseId;

        private String courseName;

        private String courseType;

        private Date scheduleDate;

        private Date startTime;

        private Date endTime;

        private Long memberId;

        private String memberName;

        private String memberMobile;

        private Long cardId;

        private String reservationType;

        private String cardName;

        private String planType;

        private Integer remainTimes;

        private Date checkinTime;


        private Integer consumeTimes;

        private BigDecimal consumeAmount;

        private Long operatorUserId;

        private String checkinStatus;

        private String derivedCheckinStatus;

        private String remark;
    }

    @Data
    public static class StudentVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long memberId;

        private String realName;

        private String nickname;

        private String mobile;

        private String avatarUrl;

        private Date lastVisitTime;

        private Long cardId;

        private String cardName;

        private Integer remainTimes;

        private BigDecimal balanceAmount;

        private Date nextClassTime;

        private String nextCourseName;

        private Long totalCheckinCount;
    }

    @Data
    public static class MessageVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String messageType;

        private String title;

        private String content;

        private Date eventTime;

        private String route;
    }

    @Data
    public static class CheckinActionVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String tenantId;

        private Long checkinId;

        private Long reservationId;

        private Long scheduleId;

        private Long storeId;

        private Long courseId;

        private String courseName;

        private Long coachId;

        private Long memberId;

        private String memberName;

        private Long cardId;

        private String reservationType;

        private String cardName;

        private String planType;

        private Long bindCoachId;

        private String cardStatus;


        private Integer remainTimes;

        private BigDecimal balanceAmount;

        private Long operatorUserId;

        private String checkinStatus;
    }
}
