package org.dromara.fitness.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 会员小程序业务对象集合
 *
 * @author CodeBuddy
 */
public final class FitnessMiniBo {

    private FitnessMiniBo() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class WechatLoginBo extends BaseEntity {

        /**
         * 小程序 wx.login() 返回的 code
         */
        private String code;

        /**
         * 微信手机号授权返回的 code，兼容保留，当前登录链路不再强依赖
         */
        private String phoneCode;

        /**
         * 兼容旧链路的 openid 直传方式
         */
        private String openid;

        private String unionId;

        private String nickname;

        private String avatarUrl;

        private String realName;

        private String mobile;

        private Long homeStoreId;

        private String remark;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class BindPhoneBo extends BaseEntity {

        /**
         * 微信手机号授权返回的 code
         */
        @NotBlank(message = "手机号授权凭证不能为空")
        private String phoneCode;

        /**
         * 后端解密后回填，不由前端直接传入
         */
        private String mobile;

        private String realName;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CourseQueryBo extends BaseEntity {

        private Long storeId;

        private String courseType;

        private String keyword;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ScheduleQueryBo extends BaseEntity {

        private Long storeId;

        private Long courseId;

        private String courseType;

        private Long coachId;

        private Date scheduleDate;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ReservationCreateBo extends BaseEntity {

        @NotNull(message = "排课ID不能为空")
        private Long scheduleId;

        private Long cardId;

        private String remark;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class OrderCreateBo extends BaseEntity {

        @NotNull(message = "卡项方案ID不能为空")
        private Long planId;

        private Long storeId;

        private Long bindCoachId;

        private Boolean payNow;

        private String payMethod;

        private String remark;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CheckinCreateBo extends BaseEntity {

        @NotNull(message = "预约ID不能为空")
        private Long reservationId;

        private Long cardId;

        private String checkinType;

        private String remark;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ProfileUpdateBo extends BaseEntity {

        private String realName;

        private String nickname;

        private String gender;

        private Date birthday;

        private String avatarUrl;

        private String remark;
    }

    @Data
    public static class OrderSettlementSnapshotBo {

        private Long orderId;

        private String orderNo;

        private BigDecimal paidAmount;

        private BigDecimal payableAmount;

        private Long cardId;
    }
}
