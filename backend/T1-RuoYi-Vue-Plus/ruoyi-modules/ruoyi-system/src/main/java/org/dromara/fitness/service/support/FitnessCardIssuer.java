package org.dromara.fitness.service.support;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.fitness.domain.FitCardPlan;
import org.dromara.fitness.domain.FitCoach;
import org.dromara.fitness.domain.FitMemberCard;
import org.dromara.fitness.mapper.FitnessCoachMapper;
import org.dromara.fitness.mapper.FitnessMemberCardMapper;
import org.dromara.fitness.mapper.FitnessStoreMapper;
import org.dromara.fitness.utils.FitnessEntitlementPolicy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FitnessCardIssuer {
    private final FitnessMemberCardMapper cards;
    private final FitnessCoachMapper coaches;
    private final FitnessStoreMapper stores;

    public void validateAssignment(String tenant, Long storeId, Long coachId, FitCardPlan plan) {
        var store = storeId == null ? null : stores.selectById(storeId);
        if (store == null || !Objects.equals(store.getTenantId(), tenant) || !"0".equals(store.getStatus()))
            throw new ServiceException("请选择在营的发卡门店");
        if (plan.getStoreId() != null && !Objects.equals(plan.getStoreId(), storeId)) throw new ServiceException("卡项方案与发卡门店不一致");
        if ("1".equals(plan.getIsPrivateBindCoach()) && coachId == null) throw new ServiceException("该卡项要求绑定教练，请选择教练后再确认");
        if (coachId != null) {
            var coach = coaches.selectOne(new LambdaQueryWrapper<FitCoach>().eq(FitCoach::getCoachId, coachId).last("FOR UPDATE"));
            if (coach == null || !Objects.equals(coach.getTenantId(), tenant) || !"0".equals(coach.getHireStatus()) || !Objects.equals(coach.getStoreId(), storeId))
                throw new ServiceException("绑定教练必须在职且属于发卡门店");
        }
    }

    public FitMemberCard issue(FitCardPlan plan, String tenant, Long memberId, Long storeId, Long coachId,
                               Date activeTime, String cardNo, Long orderId, String remark) {
        int times = FitnessEntitlementPolicy.grantedTimes(plan.getTotalTimes(), plan.getGiftTimes());
        FitMemberCard card = new FitMemberCard();
        card.setCardId(IdGeneratorUtil.nextLongId());
        card.setTenantId(tenant);
        card.setMemberId(memberId);
        card.setPlanId(plan.getPlanId());
        card.setStoreId(storeId);
        card.setBindCoachId(coachId);
        card.setCardNo(cardNo);
        card.setCardName(plan.getPlanName());
        card.setPlanType(plan.getPlanType());
        card.setActiveTime(activeTime);
        if (plan.getValidDays() != null && plan.getValidDays() > 0) card.setExpireTime(DateUtil.offsetDay(activeTime, plan.getValidDays()));
        card.setTotalTimes(times);
        card.setUsedTimes(0);
        card.setRemainTimes(times);
        card.setBalanceAmount(Objects.requireNonNullElse(plan.getStoredAmount(), BigDecimal.ZERO));
        card.setCardStatus("active");
        card.setOrderId(orderId);
        card.setRemark(remark);
        if (cards.insert(card) != 1) throw new ServiceException("会员卡发放失败");
        return card;
    }
}
