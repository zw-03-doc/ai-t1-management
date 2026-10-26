package org.dromara.fitness.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 教练 App 业务对象集合
 *
 * @author CodeBuddy
 */
public final class FitnessCoachAppBo {

    private FitnessCoachAppBo() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class LoginBo extends BaseEntity {

        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class WechatLoginBo extends BaseEntity {

        /**
         * 小程序 wx.login() 返回的 code
         */
        private String code;

        /**
         * 兼容旧链路的 openid 直传方式
         */
        private String openid;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class WechatActivateBo extends BaseEntity {

        /**
         * 小程序 wx.login() 返回的 code
         */
        private String code;

        /**
         * 兼容旧链路的 openid 直传方式
         */
        private String openid;

        @NotBlank(message = "手机号不能为空")
        private String mobile;

        @NotBlank(message = "激活码不能为空")
        private String activationCode;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ScheduleQueryBo extends BaseEntity {

        /** today / tomorrow / week / all */
        private String scope;

        private Date scheduleDate;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CheckinQueryBo extends BaseEntity {

        /** pending / confirmed / exception / all */
        private String status;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CheckinConfirmBo extends BaseEntity {
        @jakarta.validation.constraints.Pattern(regexp = "mini|web")
        private String source;

        @NotNull(message = "预约ID不能为空")
        private Long reservationId;

        private Integer consumeTimes;

        private BigDecimal consumeAmount;

        private String remark;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CheckinExceptionBo extends BaseEntity {
        @jakarta.validation.constraints.Pattern(regexp = "mini|web")
        private String source;

        @NotNull(message = "预约ID不能为空")
        private Long reservationId;

        private String remark;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ProfileUpdateBo extends BaseEntity {

        private String nickName;

        private String coachName;

        private String mobile;

        private String avatarUrl;

        private String skillTags;

        private String intro;

        private String remark;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PasswordUpdateBo extends BaseEntity {

        @NotBlank(message = "旧密码不能为空")
        private String oldPassword;

        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }
}
