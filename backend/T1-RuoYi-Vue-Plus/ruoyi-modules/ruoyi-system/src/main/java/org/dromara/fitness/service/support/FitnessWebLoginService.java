package org.dromara.fitness.service.support;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.redisson.api.RedissonClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.transaction.annotation.Transactional;
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
    private final RedissonClient redis;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generate() {
        var user = FitnessSecurityUtils.getMemberLoginUser();
        FitMember member = memberMapper.selectOne(new LambdaQueryWrapper<FitMember>().eq(FitMember::getMemberId, user.getMemberId()).last("FOR UPDATE"));
        if (member == null || !Objects.equals(member.getTenantId(), user.getTenantId()) || !"normal".equals(member.getMemberStatus()))
            throw new ServiceException("会员已停用或不存在");
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String code = HexFormat.of().formatHex(bytes);
        var owner = redis.<String>getBucket("fitness:web-login-owner:" + user.getMemberId());
        String previous = owner.get();
        if (previous != null) redis.getBucket(PREFIX + previous).delete();
        redis.<String>getBucket(PREFIX + code).set(user.getTenantId() + ":" + user.getMemberId() + ":v2", Duration.ofSeconds(120));
        owner.set(code, Duration.ofSeconds(120));
        return Map.of("code", code, "expiresIn", 120);
    }

    @Transactional(rollbackFor = Exception.class)
    public FitnessMiniVo.LoginVo login(String code) {
        if (code == null || !code.matches("[a-f0-9]{32}")) throw new ServiceException("网页登录码格式不正确");
        String identity = redis.<String>getBucket(PREFIX + code).getAndDelete();
        if (identity == null) throw new ServiceException("网页登录码已使用或已过期，请在微信中重新生成");
        String[] parts = identity.split(":");
        if (parts.length != 3 || !"v2".equals(parts[2])) throw new ServiceException("网页登录码已失效，请重新生成");
        return TenantHelper.dynamic(parts[0], () -> {
            Long memberId = Long.valueOf(parts[1]);
            FitMember member = memberMapper.selectOne(new LambdaQueryWrapper<FitMember>().eq(FitMember::getMemberId, memberId).last("FOR UPDATE"));
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
