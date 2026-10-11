package org.dromara.fitness.constant;

/**
 * 健身房移动端常量
 *
 * @author CodeBuddy
 */
public interface FitnessMobileConstants {

    /** 默认会员端客户端标识 */
    String MINI_CLIENT_ID = "fitness-mini";

    /** 默认教练端客户端标识 */
    String COACH_CLIENT_ID = "fitness-coach-app";

    /** 默认 token 过期时间：7 天 */
    long DEFAULT_TIMEOUT = 7 * 24 * 60 * 60L;

    /** 默认 token 活跃过期时间：30 分钟 */
    long DEFAULT_ACTIVE_TIMEOUT = 30 * 60L;

    /** 会员登录态 userId 偏移量，避免与 sys_user 主键冲突 */
    long MEMBER_LOGIN_ID_OFFSET = 1_000_000_000_000L;

    /** 签到待教练确认 */
    String CHECKIN_PENDING_CONFIRM = "pending_confirm";

    /** 签到已确认 */
    String CHECKIN_CONFIRMED = "confirmed";

    /** 签到异常 */
    String CHECKIN_EXCEPTION = "exception";

    /** 尚未签到 */
    String CHECKIN_NOT_STARTED = "not_checkin";
}
