package org.dromara.fitness.service.support;

import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.fitness.config.properties.FitnessWechatMiniProperties;
import org.dromara.fitness.domain.dto.FitnessWechatMiniDto;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信小程序认证支持服务
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessWechatMiniAuthService {

    private static final String CODE2_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String STABLE_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/stable_token";
    private static final String PHONE_NUMBER_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

    private final FitnessWechatMiniProperties properties;

    public FitnessWechatMiniDto.SessionInfo code2Session(String code) {
        validateConfig();
        if (StringUtils.isBlank(code)) {
            throw new ServiceException("微信登录凭证不能为空");
        }
        try {
            String body = HttpRequest.get(CODE2_SESSION_URL)
                .form("appid", properties.getAppid())
                .form("secret", properties.getSecret())
                .form("js_code", code)
                .form("grant_type", "authorization_code")
                .timeout(10000)
                .execute()
                .body();
            WechatSessionResponse response = JsonUtils.parseObject(body, WechatSessionResponse.class);
            ensureSuccess(response, "获取微信登录态失败");
            if (StringUtils.isBlank(response.getOpenid())) {
                throw new ServiceException("微信登录失败，未获取到 openid");
            }
            FitnessWechatMiniDto.SessionInfo result = new FitnessWechatMiniDto.SessionInfo();
            result.setOpenid(response.getOpenid());
            result.setUnionId(response.getUnionId());
            result.setSessionKey(response.getSessionKey());
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("请求微信登录接口失败：" + e.getMessage());
        }
    }

    public FitnessWechatMiniDto.PhoneInfo getPhoneInfo(String phoneCode) {
        validateConfig();
        if (StringUtils.isBlank(phoneCode)) {
            throw new ServiceException("手机号授权凭证不能为空");
        }
        try {
            String accessToken = getAccessToken();
            Map<String, String> payload = new HashMap<>(1);
            payload.put("code", phoneCode);
            String body = HttpRequest.post(PHONE_NUMBER_URL + "?access_token=" + accessToken)
                .body(JsonUtils.toJsonString(payload), ContentType.JSON.getValue())
                .timeout(10000)
                .execute()
                .body();
            WechatPhoneResponse response = JsonUtils.parseObject(body, WechatPhoneResponse.class);
            ensureSuccess(response, "获取微信手机号失败");
            if (response.getPhoneInfo() == null || StringUtils.isBlank(response.getPhoneInfo().getPhoneNumber())) {
                throw new ServiceException("获取微信手机号失败，手机号为空");
            }
            FitnessWechatMiniDto.PhoneInfo result = new FitnessWechatMiniDto.PhoneInfo();
            result.setPhoneNumber(response.getPhoneInfo().getPhoneNumber());
            result.setPurePhoneNumber(response.getPhoneInfo().getPurePhoneNumber());
            result.setCountryCode(response.getPhoneInfo().getCountryCode());
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("请求微信手机号接口失败：" + e.getMessage());
        }
    }

    private String getAccessToken() {
        Map<String, String> payload = new HashMap<>(4);
        payload.put("grant_type", "client_credential");
        payload.put("appid", properties.getAppid());
        payload.put("secret", properties.getSecret());
        String body = HttpRequest.post(STABLE_TOKEN_URL)
            .body(JsonUtils.toJsonString(payload), ContentType.JSON.getValue())
            .timeout(10000)
            .execute()
            .body();
        WechatAccessTokenResponse response = JsonUtils.parseObject(body, WechatAccessTokenResponse.class);
        ensureSuccess(response, "获取微信 access_token 失败");
        if (StringUtils.isBlank(response.getAccessToken())) {
            throw new ServiceException("获取微信 access_token 失败，返回为空");
        }
        return response.getAccessToken();
    }

    private void validateConfig() {
        if (StringUtils.isBlank(properties.getAppid()) || StringUtils.isBlank(properties.getSecret())) {
            throw new ServiceException("请先配置 fitness.wechat.mini.appid 与 fitness.wechat.mini.secret");
        }
    }

    private void ensureSuccess(WechatBaseResponse response, String defaultMessage) {
        if (response == null) {
            throw new ServiceException(defaultMessage + "，微信返回为空");
        }
        if (response.getErrcode() != null && response.getErrcode() != 0) {
            String message = StringUtils.isBlank(response.getErrmsg()) ? defaultMessage : response.getErrmsg();
            throw new ServiceException(defaultMessage + "：" + message);
        }
    }

    @Data
    private static class WechatBaseResponse {

        private Integer errcode;

        private String errmsg;
    }

    @Data
    private static class WechatSessionResponse extends WechatBaseResponse {

        private String openid;

        @JsonProperty("unionid")
        private String unionId;

        @JsonProperty("session_key")
        private String sessionKey;
    }

    @Data
    private static class WechatAccessTokenResponse extends WechatBaseResponse {

        @JsonProperty("access_token")
        private String accessToken;
    }

    @Data
    private static class WechatPhoneResponse extends WechatBaseResponse {

        @JsonProperty("phone_info")
        private WechatPhoneInfo phoneInfo;
    }

    @Data
    private static class WechatPhoneInfo {

        private String phoneNumber;

        private String purePhoneNumber;

        private String countryCode;
    }
}
