package org.dromara.fitness.domain.dto;

import lombok.Data;

/**
 * 微信小程序认证 DTO
 *
 * @author CodeBuddy
 */
public final class FitnessWechatMiniDto {

    private FitnessWechatMiniDto() {
    }

    @Data
    public static class SessionInfo {

        private String openid;

        private String unionId;

        private String sessionKey;
    }

    @Data
    public static class PhoneInfo {

        private String phoneNumber;

        private String purePhoneNumber;

        private String countryCode;
    }
}
