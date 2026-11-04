package org.dromara.fitness.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.*;
import org.dromara.fitness.mapper.*;
import org.dromara.fitness.utils.FitnessEntitlementPolicy;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FitnessEntitlementService {
    private final FitnessMemberCardMapper cards;
    private final FitnessCardPlanMapper plans;
    private final FitnessCourseMapper courses;
    private final FitnessScheduleMapper schedules;
    public record ReservationRule(Long cardId, String reservationType, String consumeMode) { }

    public ReservationRule reservationRule(Long memberId, Long scheduleId, Long cardId, boolean experience) {
        if (cardId == null) {
            if (!experience) throw new ServiceException("请选择可用会员卡，或明确选择无卡体验");
            return new ReservationRule(null, "store_experience", "free");
        }
        if (experience) throw new ServiceException("卡项预约不能同时选择无卡体验");
        FitMemberCard card = cards.selectById(cardId);
        validate(card, memberId, scheduleId);
        return new ReservationRule(cardId, "card", FitnessEntitlementPolicy.mode(card.getPlanType()));
    }

    public List<FitMemberCard> eligibleCards(Long memberId, Long scheduleId) {
        return cards.selectList(new LambdaQueryWrapper<FitMemberCard>()
            .eq(FitMemberCard::getTenantId, FitnessSecurityUtils.getCurrentTenantId()).eq(FitMemberCard::getMemberId, memberId))
            .stream().filter(card -> { try { validate(card, memberId, scheduleId); return true; } catch (ServiceException ex) { return false; } }).toList();
    }

    private void validate(FitMemberCard card, Long memberId, Long scheduleId) {
        FitCourseSchedule schedule = schedules.selectById(scheduleId);
        String tenant = FitnessSecurityUtils.getCurrentTenantId();
        if (schedule == null || !Objects.equals(schedule.getTenantId(), tenant)) throw new ServiceException("场次不存在");
        FitCourse course = courses.selectById(schedule.getCourseId());
        if (course == null || !Objects.equals(course.getTenantId(), tenant)) throw new ServiceException("课程不存在");
        FitnessEntitlementPolicy.validate(card, card == null ? null : plans.selectById(card.getPlanId()), course, schedule, memberId, tenant, new Date());
    }

    /** Caller holds the reservation lock and a transaction; card lock protects cross-session spending. */
    public FitnessEntitlementPolicy.Consumption consume(FitCourseReservation reservation, Integer times, BigDecimal amount) {
        FitMemberCard card = reservation.getCardId() == null ? null : cards.selectOne(new LambdaQueryWrapper<FitMemberCard>()
            .eq(FitMemberCard::getCardId, reservation.getCardId()).eq(FitMemberCard::getTenantId, FitnessSecurityUtils.getCurrentTenantId()).last("FOR UPDATE"));
        if (reservation.getCardId() != null) validate(card, reservation.getMemberId(), reservation.getScheduleId());
        var usage = FitnessEntitlementPolicy.consumption(reservation.getCardId(), card == null ? null : card.getPlanType(), times, amount);
        if (card == null) return usage;
        int remaining = card.getRemainTimes() == null ? 0 : card.getRemainTimes();
        BigDecimal balance = card.getBalanceAmount() == null ? BigDecimal.ZERO : card.getBalanceAmount();
        if (usage.times() > remaining || usage.amount().compareTo(balance) > 0) throw new ServiceException("会员卡剩余权益不足");
        FitMemberCard update = new FitMemberCard(); update.setCardId(card.getCardId()); update.setVersion(card.getVersion());
        update.setUsedTimes((card.getUsedTimes() == null ? 0 : card.getUsedTimes()) + usage.times());
        update.setRemainTimes(remaining - usage.times()); update.setBalanceAmount(balance.subtract(usage.amount()));
        String mode = FitnessEntitlementPolicy.mode(card.getPlanType());
        boolean exhausted = "times".equals(mode) && remaining == usage.times() || "amount".equals(mode) && balance.compareTo(usage.amount()) == 0;
        update.setCardStatus(exhausted ? "exhausted" : "active");
        if (cards.updateById(update) != 1) throw new ServiceException("会员卡权益已变化，请刷新后重试");
        return usage;
    }
}
