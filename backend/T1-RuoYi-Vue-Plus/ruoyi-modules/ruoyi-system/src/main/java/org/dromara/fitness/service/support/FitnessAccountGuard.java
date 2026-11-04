package org.dromara.fitness.service.support;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.domain.model.FitnessLoginUser;
import org.dromara.fitness.mapper.FitnessMemberMapper;
import org.dromara.fitness.mapper.FitnessCoachMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FitnessAccountGuard {
    private final FitnessMemberMapper members;
    private final FitnessCoachMapper coaches;
    private final SysUserMapper users;

    public void check(Object principal) {
        if (principal instanceof FitnessLoginUser.MemberLoginUser login) {
            var member = members.selectById(login.getMemberId());
            if (member == null || !Objects.equals(member.getTenantId(), login.getTenantId()) || !"normal".equals(member.getMemberStatus())) {
                FitnessSessionRevoker.member(login.getMemberId());
                throw new ServiceException("会员已冻结、流失或删除，请联系门店后重新登录", 401);
            }
        } else if (principal instanceof FitnessLoginUser.CoachLoginUser login) {
            var coach = coaches.selectById(login.getCoachId());
            var user = users.selectById(login.getUserId());
            if (coach == null || user == null || !Objects.equals(coach.getTenantId(), login.getTenantId())
                || !Objects.equals(user.getTenantId(), login.getTenantId()) || !Objects.equals(coach.getUserId(), login.getUserId())
                || !"0".equals(coach.getHireStatus()) || !"0".equals(user.getStatus())) {
                FitnessSessionRevoker.coach(login.getUserId());
                throw new ServiceException("教练已停用或删除，请联系管理员后重新登录", 401);
            }
        }
    }
}
