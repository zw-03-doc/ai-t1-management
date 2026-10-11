package org.dromara.fitness.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.core.domain.model.LoginUser;

import java.io.Serial;

/**
 * 健身房移动端登录用户扩展模型
 *
 * @author CodeBuddy
 */
public final class FitnessLoginUser {

    private FitnessLoginUser() {
    }

    /**
     * 会员小程序登录用户
     */
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class MemberLoginUser extends LoginUser {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long memberId;

        private Long homeStoreId;

        private String openid;
    }

    /**
     * 教练 App 登录用户
     */
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class CoachLoginUser extends LoginUser {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long coachId;

        private Long storeId;
    }
}
