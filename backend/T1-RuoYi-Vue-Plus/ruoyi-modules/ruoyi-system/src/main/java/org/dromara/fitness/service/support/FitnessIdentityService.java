package org.dromara.fitness.service.support;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.fitness.domain.FitMember;
import org.dromara.fitness.domain.bo.FitnessMiniBo;
import org.dromara.fitness.mapper.FitnessIdentityMapper;
import org.dromara.fitness.mapper.FitnessMiniMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FitnessIdentityService {
    private final FitnessIdentityMapper identities;
    private final FitnessMiniMapper mini;
    private final FitnessMemberAvatarService avatars;

    /** The phone is verified by WeChat on the server before entering this transaction. No business records are merged. */
    @Transactional(rollbackFor = Exception.class)
    public Long bindVerifiedPhone(String tenant, Long memberId, String sessionOpenid, FitnessMiniBo.BindPhoneBo bo) {
        FitMember source = identities.lockMember(tenant, memberId);
        if (source == null || !"normal".equals(source.getMemberStatus()) || StringUtils.isBlank(sessionOpenid)
            || !Objects.equals(source.getWxOpenid(), sessionOpenid)) throw new ServiceException("请使用本人微信重新登录后绑定手机号");
        var owners = identities.lockPhoneOwners(tenant, bo.getMobile());
        if (owners.size() > 1) throw new ServiceException("手机号对应多个会员档案，请联系门店核对");
        FitMember target = owners.isEmpty() ? null : owners.get(0);
        if (target == null || Objects.equals(target.getMemberId(), memberId)) {
            if (StringUtils.isNotBlank(source.getMobile()) && !Objects.equals(source.getMobile(), bo.getMobile()))
                throw new ServiceException("已绑定手机号不可直接更换，请联系门店核对身份");
            if (mini.updateMemberPhone(memberId, bo) != 1) throw new ServiceException("绑定失败，请刷新重试");
            return memberId;
        }
        if (!"normal".equals(target.getMemberStatus()) || StringUtils.isNotBlank(target.getWxOpenid()) || !"admin".equals(target.getRegisterSource()))
            throw new ServiceException("该手机号档案已被认领或已停用，请联系门店核对身份");
        if (!"mini".equals(source.getRegisterSource()) || StringUtils.isNotBlank(source.getMobile())
            || identities.countMemberBusiness(tenant, memberId) != 0)
            throw new ServiceException("当前微信会员已有业务记录，不能自动合并，请联系门店核对两份档案");
        String avatar = null;
        if (StringUtils.isBlank(target.getAvatarUrl()) && StringUtils.isNotBlank(source.getAvatarUrl())) {
            avatar = avatars.copyForClaim(memberId, source.getAvatarUrl(), target.getMemberId());
        }
        if (identities.retireBlankIdentity(tenant, memberId) != 1
            || identities.claimIdentity(tenant, target.getMemberId(), source, avatar) != 1)
            throw new ServiceException("会员认领状态已变化，请刷新重试");
        FitnessSessionRevoker.member(tenant, memberId);
        return target.getMemberId();
    }
}
