package org.dromara.fitness.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.function.Supplier;

/**
 * 乐观锁冲突重试工具
 */
@Slf4j
public final class FitnessRetryUtils {

    private FitnessRetryUtils() {
    }

    /**
     * 乐观锁冲突时自动重试，默认最多 3 次
     *
     * @param fn 业务操作（需包含重新查询+更新的完整逻辑）
     */
    public static <T> T retryOnConflict(Supplier<T> fn) {
        return retryOnConflict(3, 100, fn);
    }

    /**
     * 乐观锁冲突时自动重试
     *
     * @param maxRetries 最大重试次数
     * @param baseDelay  初始延迟毫秒（每次翻倍）
     * @param fn         业务操作
     */
    public static <T> T retryOnConflict(int maxRetries, long baseDelay, Supplier<T> fn) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                return fn.get();
            } catch (OptimisticLockingFailureException e) {
                if (i == maxRetries - 1) {
                    throw e;
                }
                long delay = baseDelay * (1L << i);
                log.warn("乐观锁冲突，第{}次重试（延迟{}ms）", i + 1, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试被中断", ie);
                }
            }
        }
        // 理论上不会到达此处（循环内要么 return 要么 throw），但保持编译安全
        throw new OptimisticLockingFailureException("重试已耗尽，请稍后重试");
    }
}
