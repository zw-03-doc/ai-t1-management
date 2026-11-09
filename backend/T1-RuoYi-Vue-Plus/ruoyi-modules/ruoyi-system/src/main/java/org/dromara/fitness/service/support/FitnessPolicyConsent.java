package org.dromara.fitness.service.support;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.vo.FitnessCoachAppVo;
import org.dromara.fitness.domain.vo.FitnessMiniVo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** The version covers both the service agreement and privacy policy. */
@Service
@RequiredArgsConstructor
public class FitnessPolicyConsent {
    public static final String VERSION = "2026-11-09";
    private final JdbcTemplate jdbc;

    public void requireCurrent(String acceptedVersion) {
        if (!VERSION.equals(acceptedVersion)) {
            throw new ServiceException("请使用最新版本，阅读并同意用户服务协议和隐私政策后再登录");
        }
    }

    public FitnessMiniVo.LoginVo recordMember(FitnessMiniVo.LoginVo login) {
        var member = login.getMemberInfo();
        record(member.getTenantId(), "member", member.getMemberId());
        return login;
    }

    public FitnessCoachAppVo.LoginVo recordCoach(FitnessCoachAppVo.LoginVo login) {
        if (!Boolean.TRUE.equals(login.getNeedActivation())) {
            var coach = login.getProfile();
            record(coach.getTenantId(), "coach", coach.getCoachId());
        }
        return login;
    }

    private void record(String tenantId, String role, Long accountId) {
        jdbc.update("""
            INSERT INTO fit_policy_consent (tenant_id, account_role, account_id, policy_version)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE last_agreed_at = CURRENT_TIMESTAMP
            """, tenantId, role, accountId, VERSION);
    }
}
