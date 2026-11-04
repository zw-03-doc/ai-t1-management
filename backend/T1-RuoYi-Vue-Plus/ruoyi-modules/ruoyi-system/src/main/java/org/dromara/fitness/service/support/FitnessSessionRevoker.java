package org.dromara.fitness.service.support;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.fitness.constant.FitnessMobileConstants;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Revoke every device only after a successful database commit. */
public final class FitnessSessionRevoker {
    private FitnessSessionRevoker() {}
    public static void member(Long id) {
        afterCommit(() -> {
            StpUtil.logout("app_user:" + (FitnessMobileConstants.MEMBER_LOGIN_ID_OFFSET + id));
            String code = RedisUtils.getClient().<String>getBucket("fitness:web-login-owner:" + id).getAndDelete();
            if (code != null) RedisUtils.deleteObject("fitness:web-login:" + code);
        });
    }
    public static void coach(Long userId) {
        if (userId == null) return;
        afterCommit(() -> { StpUtil.logout("app_user:" + userId); StpUtil.logout("sys_user:" + userId); });
    }
    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { action.run(); }
            });
        } else action.run();
    }
}
