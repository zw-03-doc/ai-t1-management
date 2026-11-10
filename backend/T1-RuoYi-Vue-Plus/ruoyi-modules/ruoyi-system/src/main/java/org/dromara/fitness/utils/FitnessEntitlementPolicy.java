package org.dromara.fitness.utils;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

/** One policy for member/admin bookings and coach/admin redemption. */
public final class FitnessEntitlementPolicy {
    private FitnessEntitlementPolicy() { }
    public record Consumption(int times, BigDecimal amount) { }

    public static int grantedTimes(Integer base, Integer gift) {
        int times = Objects.requireNonNullElse(base, 0), bonus = Objects.requireNonNullElse(gift, 0);
        if (times < 0 || bonus < 0 || (long) times + bonus > Integer.MAX_VALUE)
            throw new ServiceException("次数和赠送次数须为非负整数，总次数不能超过 2147483647");
        return times + bonus;
    }

    public static String mode(String planType) {
        if ("stored_card".equals(planType)) return "amount";
        if ("time_card".equals(planType)) return "free";
        return "times";
    }

    public static void validate(FitMemberCard card, FitCardPlan plan, FitCourse course,
                                FitCourseSchedule schedule, Long memberId, String tenant, Date now) {
        if (card == null || !Objects.equals(card.getTenantId(), tenant) || !Objects.equals(card.getMemberId(), memberId))
            throw new ServiceException("会员卡不存在或不属于当前会员");
        if (!Set.of("unused", "active").contains(Objects.toString(card.getCardStatus(), "")))
            throw new ServiceException("会员卡已冻结、过期或用尽");
        Date classTime = schedule.getStartTime();
        if (card.getActiveTime() != null && (now.before(card.getActiveTime()) || classTime.before(card.getActiveTime())))
            throw new ServiceException("会员卡尚未生效");
        if (card.getExpireTime() != null && (!now.before(card.getExpireTime()) || !classTime.before(card.getExpireTime())))
            throw new ServiceException("会员卡在上课时已过期");
        if (card.getStoreId() != null && !Objects.equals(card.getStoreId(), schedule.getStoreId()))
            throw new ServiceException("会员卡不适用于本门店");
        if (card.getBindCoachId() != null && !Objects.equals(card.getBindCoachId(), schedule.getCoachId()))
            throw new ServiceException("会员卡绑定教练与授课教练不一致");
        if (plan != null && !allows(plan.getSupportCourseType(), course.getCourseType()))
            throw new ServiceException("会员卡不支持此课程类型");
        if (!allows(course.getSupportCardType(), card.getPlanType()))
            throw new ServiceException("课程不支持此会员卡类型");
        if ("times".equals(mode(card.getPlanType())) && (card.getRemainTimes() == null || card.getRemainTimes() <= 0))
            throw new ServiceException("会员卡课次不足");
        if ("amount".equals(mode(card.getPlanType())) && (card.getBalanceAmount() == null || card.getBalanceAmount().signum() <= 0))
            throw new ServiceException("会员卡余额不足");
    }

    private static boolean allows(String configured, String value) {
        return configured == null || configured.isBlank() || Arrays.stream(configured.split("[,，;\\s]+")).anyMatch(v -> v.equals(value) || v.equals("all"));
    }

    public static Consumption consumption(Long cardId, String planType, Integer requestedTimes, BigDecimal requestedAmount) {
        String mode = cardId == null ? "free" : mode(planType);
        int times = requestedTimes == null ? ("times".equals(mode) ? 1 : 0) : requestedTimes;
        BigDecimal amount = requestedAmount == null ? BigDecimal.ZERO : requestedAmount;
        if (times < 0 || amount.signum() < 0 || amount.stripTrailingZeros().scale() > 2)
            throw new ServiceException("核销课次不得为负，金额须为最多两位小数");
        if ("amount".equals(mode) && (times != 0 || amount.signum() <= 0))
            throw new ServiceException("储值卡须填写实际扣款金额，不扣课次");
        if ("times".equals(mode) && (times <= 0 || amount.signum() != 0))
            throw new ServiceException("次卡须填写正整数课次，不扣金额");
        if ("free".equals(mode) && (times != 0 || amount.signum() != 0))
            throw new ServiceException("期限卡或无卡体验不扣课次和金额");
        return new Consumption(times, amount);
    }
}
