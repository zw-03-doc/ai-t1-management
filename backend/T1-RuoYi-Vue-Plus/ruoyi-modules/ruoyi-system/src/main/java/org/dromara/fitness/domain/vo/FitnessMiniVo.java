package org.dromara.fitness.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 会员小程序视图对象集合
 *
 * @author CodeBuddy
 */
public final class FitnessMiniVo {

    private FitnessMiniVo() {
    }

    @Data
    public static class LoginVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String accessToken;

        private Long expireIn;

        private String clientId;

        private String openid;

        private MemberInfoVo memberInfo;
    }

    @Data
    public static class MemberInfoVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String tenantId;

        private Long memberId;

        private String memberNo;

        private Long homeStoreId;

        private String mobile;

        private String realName;

        private String nickname;

        private String avatarUrl;

        private String memberStatus;

        public String getDisplayName() {
            if (realName != null && !realName.isBlank()) {
                return realName;
            }
            if (nickname != null && !nickname.isBlank()) {
                return nickname;
            }
            return "会员" + memberId;
        }
    }

    @Data
    public static class HomeIndexVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private MineSummaryVo mineSummary;

        private List<String> bannerList;

        private List<CourseVo> recommendCourseList;

        private List<ReservationVo> upcomingReservationList;

        private List<MemberCardVo> cardList;

        private List<CardPlanVo> promotionList;
    }

    @Data
    public static class StoreVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long storeId;

        private String storeCode;

        private String storeName;

        private String storeShortName;

        private String contactName;

        private String contactPhone;

        private String address;

        private String businessHours;

        private String coverUrl;

        private String status;

        private Boolean homeStore;
    }

    @Data
    public static class CourseVo implements Serializable {

    /** Display-only price in CNY; no online payment is created. */
    private java.math.BigDecimal displayPrice;
    private Integer listingDays;
    private java.util.Date publishedAt;
    private java.util.Date listingUntil;
    private Integer bookingAdvanceDays;
    private Integer bookingCloseMinutes;
    private Integer cancelCloseMinutes;
    private Integer checkinBeforeMinutes;
    private Integer checkinAfterMinutes;

        private String coachContactWechat;


        @Serial
        private static final long serialVersionUID = 1L;

        private Long courseId;

        private String courseName;

        private String courseType;

        private Long storeId;

        private String storeName;

        private Long defaultCoachId;

        private String coachName;

        private Integer durationMinutes;

        private Integer capacity;

        private String coverUrl;

        private String description;

        private String supportCardType;

        private String status;
    }

    @Data
    public static class ScheduleVo implements Serializable {

    /** Price captured when scheduling. */
    private java.math.BigDecimal displayPrice;
    private java.util.Date reservationOpenTime;

        private Date reservationDeadline;
        private Date cancelDeadline;


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

        private Integer availableCount;

        private String scheduleStatus;

        private Boolean reservedByMe;

        private Long reservationId;

        private String reservationStatus;

        private String checkinStatus;

        private String derivedCheckinStatus;
    }

    @Data
    public static class ReservationVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long reservationId;

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

        private Long cardId;

        private String cardName;

        private String reservationStatus;

        private Date reservationTime;

        private Date cancelTime;

        private String cancelReason;

        private Long checkinId;

        private Date checkinTime;

        private Long operatorUserId;

        private String checkinStatus;

        private String derivedCheckinStatus;
    }

    @Data
    public static class ActivityVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long activityId;

        private String activityNo;

        private Long storeId;

        private String storeName;

        private String title;

        private String subTitle;

        private String priceHighlight;

        private String targetAudience;

        private String coverUrl;

        private String detailImageUrl;

        private String detailContent;

        private Date startTime;

        private Date endTime;

        private Date countdownEndTime;

        private String weeklyFeatured;

        private String activityType;

        private String actionType;

        private String discountLabel;

        private String ruleNote;

        private String storeContactPhone;

        private Integer sortNo;

        private String status;

        private List<ActivityCardPlanVo> planList;
    }

    @Data
    public static class ActivityCardPlanVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long planId;

        private Long storeId;

        private String storeName;

        private String planName;

        private String planType;

        private BigDecimal salePrice;

        private BigDecimal originalPrice;

        private Integer validDays;

        private Integer totalTimes;

        private Integer giftTimes;

        private BigDecimal storedAmount;

        private String supportCourseType;

        private String isPrivateBindCoach;

        private String status;

        private String discountLabel;

        private String ruleNote;
    }

    @Data
    public static class CardPlanVo implements Serializable {



        @Serial
        private static final long serialVersionUID = 1L;

        private Long planId;

        private Long storeId;

        private String storeName;

        private String planName;

        private String planType;

        private BigDecimal salePrice;

        private BigDecimal originalPrice;

        private Integer validDays;

        private Integer totalTimes;

        private Integer giftTimes;

        private BigDecimal storedAmount;

        private String supportCourseType;

        private String isPrivateBindCoach;

        private String status;
    }

    @Data
    public static class MemberCardVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long cardId;

        private Long planId;

        private String cardNo;

        private String cardName;

        private String planType;

        private Long bindCoachId;

        private String coachName;

        private Date activeTime;

        private Date expireTime;

        private Integer totalTimes;

        private Integer usedTimes;

        private Integer remainTimes;

        private BigDecimal balanceAmount;

        private String cardStatus;
    }

    @Data
    public static class OrderVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long orderId;

        private String orderNo;

        private Long storeId;

        private String storeName;

        private String orderType;

        private String orderSource;

        private String orderStatus;

        private String payMethod;

        private BigDecimal totalAmount;

        private BigDecimal discountAmount;

        private BigDecimal payableAmount;

        private BigDecimal paidAmount;

        private Date paidTime;

        private Date createTime;

        private List<OrderItemVo> itemList;
    }

    @Data
    public static class OrderItemVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long itemId;

        private String itemNo;

        private String itemType;


        private Long itemRefId;

        private String itemName;

        private Integer quantity;

        private BigDecimal unitPrice;

        private BigDecimal totalPrice;

        private BigDecimal discountAmount;

        private BigDecimal realAmount;

        private Long grantCardId;
    }

    @Data
    public static class CheckinVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long checkinId;

        private Long reservationId;

        private Long scheduleId;

        private Long memberId;

        private String memberName;

        private String courseName;

        private String coachName;

        private Long cardId;

        private String cardName;

        private Date scheduleDate;

        private Date startTime;

        private Date endTime;

        private Date checkinTime;

        private Integer consumeTimes;

        private BigDecimal consumeAmount;

        private Long operatorUserId;

        private String checkinStatus;

        private String derivedCheckinStatus;

        private String remark;
    }

    @Data
    public static class MineSummaryVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long memberId;

        private String realName;

        private String nickname;

        private String mobile;

        private String avatarUrl;

        private Long totalReservationCount;

        private Long pendingReservationCount;

        private Long totalCardCount;

        private Long activeCardCount;

        private Long pendingConfirmCount;

        private Long confirmedCheckinCount;
    }

    @Data
    public static class ProfileVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long memberId;

        private String memberNo;

        private Long homeStoreId;

        private String mobile;

        private String realName;

        private String nickname;

        private String gender;

        private Date birthday;

        private String avatarUrl;

        private String memberStatus;

        private String remark;
    }

    @Data
    public static class ReservationActionVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String tenantId;

        private Long reservationId;

        private Long storeId;

        private Long scheduleId;

        private Long memberId;

        private Long cardId;

        private String reservationType;

        private String planType;

        private Long bindCoachId;

        private String storeName;


        private Long courseId;

        private String courseName;

        private String courseType;

        private Long coachId;

        private String coachName;

        private Date scheduleDate;

        private Date startTime;

        private Date endTime;

        private Date reservationDeadline;

        private Date cancelDeadline;

        private String scheduleStatus;

        private String reservationStatus;

        private Integer capacity;

        private Integer reservedCount;
    }
}

