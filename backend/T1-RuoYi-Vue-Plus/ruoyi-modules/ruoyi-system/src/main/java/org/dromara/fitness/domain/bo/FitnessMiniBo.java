package org.dromara.fitness.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WechatLoginBo {

        /**
         * 小程序 wx.login() 返回的 code
         */
        @NotBlank(message = "请通过微信登录凭证登录")
        private String code;
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

        private Boolean experience;

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
        @jakarta.validation.constraints.Pattern(regexp = "mini|web")
        private String source;

        @NotNull(message = "预约ID不能为空")
        private Long reservationId;

        private Long cardId;

        private String checkinType;

        private String remark;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ProfileUpdateBo extends BaseEntity {

        @Size(max = 50, message = "姓名最多50个字符")
        private String realName;

        @Size(max = 50, message = "昵称最多50个字符")
        private String nickname;

        @Pattern(regexp = "[012]", message = "请选择有效的性别")
        private String gender;

        private Date birthday;

        private Boolean clearBirthday;

        @Pattern(regexp = "(?:|1[3-9]\\d{9}|\\+[1-9]\\d{6,14})", message = "请输入有效的联系电话")
        private String contactMobile;

        @Pattern(regexp = "skip|complete", message = "请选择有效的资料设置操作")
        private String profileSetupAction;

        @Size(max = 255, message = "头像地址过长")
        private String avatarUrl;

        @Size(max = 500, message = "备注最多500个字符")
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
