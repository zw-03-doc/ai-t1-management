package org.dromara.fitness.service.support;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.constant.FitnessMobileConstants;
import org.dromara.fitness.domain.FitMember;
import org.dromara.fitness.domain.vo.FitnessMiniVo;
import org.dromara.fitness.mapper.FitnessMemberMapper;
import org.dromara.fitness.mapper.FitnessMiniMapper;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

/** A member explicitly transfers a short-lived credential from their authenticated device. */
@Service
@RequiredArgsConstructor
public class FitnessWebLoginService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "fitness:web-login:";
    private final FitnessMemberMapper memberMapper;
    private final FitnessMiniMapper miniMapper;

    public Map<String, Object> generate() {
        var user = FitnessSecurityUtils.getMemberLoginUser();
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String code = HexFormat.of().formatHex(bytes);
        RedisUtils.setCacheObject(PREFIX + code, user.getTenantId() + ":" + user.getMemberId(), Duration.ofSeconds(120));
        return Map.of("code", code, "expiresIn", 120);
    }

    public FitnessMiniVo.LoginVo login(String code) {
        if (code == null || !code.matches("[a-f0-9]{32}")) throw new ServiceException("网页登录码格式不正确");
        String identity = RedisUtils.getClient().<String>getBucket(PREFIX + code).getAndDelete();
        if (identity == null) throw new ServiceException("网页登录码已使用或已过期，请在微信中重新生成");
        String[] parts = identity.split(":", 2);
        return TenantHelper.dynamic(parts[0], () -> {
            Long memberId = Long.valueOf(parts[1]);
            FitMember member = memberMapper.selectById(memberId);
            if (member == null || !Objects.equals(member.getTenantId(), parts[0]) || !"normal".equals(member.getMemberStatus()))
                throw new ServiceException("会员已停用或不存在");
            FitnessMiniVo.MemberInfoVo info = miniMapper.selectMemberById(memberId);
            FitnessSecurityUtils.loginMember(info, member.getWxOpenid(), "web");
            FitnessMiniVo.LoginVo result = new FitnessMiniVo.LoginVo();
            result.setAccessToken(StpUtil.getTokenValue()); result.setExpireIn(StpUtil.getTokenTimeout());
            result.setClientId(FitnessMobileConstants.MINI_CLIENT_ID); result.setMemberInfo(info);
            return result;
        });
    }
}
