package org.dromara.fitness.service.impl;

import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.FitMemberCard;
import org.dromara.fitness.domain.FitMember;
import org.dromara.fitness.domain.FitCardPlan;
import org.dromara.fitness.domain.bo.FitnessCardBindBo;
import org.dromara.fitness.domain.vo.FitnessCardBindVo;
import org.dromara.fitness.mapper.FitnessMemberMapper;
import org.dromara.fitness.mapper.FitnessCardPlanMapper;
import org.dromara.fitness.service.support.FitnessCardIssuer;
import org.dromara.fitness.service.support.FitnessOperationsService;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import org.dromara.fitness.domain.bo.FitnessMemberCardBo;
import org.dromara.fitness.domain.vo.FitnessMemberCardVo;
import org.dromara.fitness.mapper.FitnessMemberCardMapper;
import org.dromara.fitness.service.IFitnessMemberCardService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 会员持卡 Service 实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessMemberCardServiceImpl implements IFitnessMemberCardService {

    private final FitnessMemberCardMapper baseMapper;
    private final FitnessMemberMapper members;
    private final FitnessCardPlanMapper plans;
    private final FitnessCardIssuer issuer;
    private final FitnessOperationsService operations;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FitnessCardBindVo bind(FitnessCardBindBo bo) {
        String tenant = FitnessSecurityUtils.getCurrentTenantId();
        var member = members.selectOne(new LambdaQueryWrapper<FitMember>()
            .eq(FitMember::getTenantId, tenant).eq(FitMember::getMemberId, bo.memberId()).last("FOR UPDATE"));
        if (member == null || !"normal".equals(member.getMemberStatus())) throw new ServiceException("请选择正常状态的会员");
        String cardNo = "MANUAL-" + bo.requestId();
        var previous = baseMapper.selectOne(new LambdaQueryWrapper<FitMemberCard>()
            .eq(FitMemberCard::getTenantId, tenant).eq(FitMemberCard::getCardNo, cardNo));
        if (previous != null) {
            if (!bo.memberId().equals(previous.getMemberId()) || !bo.planId().equals(previous.getPlanId())
                || bo.storeId() != null && !bo.storeId().equals(previous.getStoreId())
                || bo.bindCoachId() != null && !bo.bindCoachId().equals(previous.getBindCoachId()))
                throw new ServiceException("本次请求已用于其他绑卡操作，请重新打开");
            return new FitnessCardBindVo(previous.getCardId(), previous.getCardNo(), false);
        }
        var plan = plans.selectOne(new LambdaQueryWrapper<FitCardPlan>()
            .eq(FitCardPlan::getTenantId, tenant).eq(FitCardPlan::getPlanId, bo.planId()).last("FOR UPDATE"));
        if (plan == null || !"0".equals(plan.getStatus())) throw new ServiceException("请选择已上架的卡项方案");
        Long coach = bo.bindCoachId();
        if ("private_package".equals(plan.getPlanType()) || "small_group_package".equals(plan.getPlanType())) {
            if (!Arrays.asList(Objects.toString(plan.getCustomMemberIds(), "").split(",")).contains(bo.memberId().toString()))
                throw new ServiceException("该会员不在课包的指定名单中，请先修改方案");
            if (plan.getCustomCoachId() == null || coach != null && !coach.equals(plan.getCustomCoachId()))
                throw new ServiceException("请使用课包指定的教练");
            coach = plan.getCustomCoachId();
        }
        Long store = bo.storeId() != null ? bo.storeId() : (plan.getStoreId() != null ? plan.getStoreId() : member.getHomeStoreId());
        issuer.validateAssignment(tenant, store, coach, plan);
        Date now = new Date();
        var same = new LambdaQueryWrapper<FitMemberCard>()
            .eq(FitMemberCard::getTenantId, tenant).eq(FitMemberCard::getMemberId, bo.memberId())
            .eq(FitMemberCard::getStoreId, store).eq(FitMemberCard::getPlanId, bo.planId())
            .eq(coach != null, FitMemberCard::getBindCoachId, coach).isNull(coach == null, FitMemberCard::getBindCoachId)
            .in(FitMemberCard::getCardStatus, "active", "unused", "frozen")
            .and(q -> q.isNull(FitMemberCard::getExpireTime).or().gt(FitMemberCard::getExpireTime, now))
            .orderByDesc(FitMemberCard::getCardId).last("LIMIT 1 FOR UPDATE");
        var held = baseMapper.selectOne(same);
        if (held != null) {
            if ("frozen".equals(held.getCardStatus())) throw new ServiceException("会员已持有冻结的同款卡，请到会员持卡中处理");
            return new FitnessCardBindVo(held.getCardId(), held.getCardNo(), false);
        }
        var card = issuer.issue(plan, tenant, bo.memberId(), store, coach, now, cardNo, null, bo.remark());
        operations.audit(bo.memberId(), store, null, null, null, "admin_bind_member_card", "web",
            plan.getPlanName() + " · " + cardNo + (StringUtils.isBlank(bo.remark()) ? "" : " · " + bo.remark()));
        return new FitnessCardBindVo(card.getCardId(), cardNo, true);
    }

    @Override
    public FitnessMemberCardVo queryById(Long cardId) {
        FitnessMemberCardVo vo = baseMapper.selectMemberCardById(cardId);
        if (vo == null) {
            throw new ServiceException("会员卡不存在");
        }
        return vo;
    }

    @Override
    public TableDataInfo<FitnessMemberCardVo> queryPageList(FitnessMemberCardBo bo, PageQuery pageQuery) {
        return TableDataInfo.build(baseMapper.selectPageMemberCardList(pageQuery.build(), bo));
    }

    @Override
    public List<FitnessMemberCardVo> queryList(FitnessMemberCardBo bo) {
        return baseMapper.selectMemberCardList(bo);
    }

    @Override
    @Lock4j(keys = {"'card:status:' + #cardId"}, acquireTimeout = 3000)
    public Boolean updateStatus(Long cardId, String cardStatus) {
        if (StringUtils.isBlank(cardStatus)) {
            throw new ServiceException("卡状态不能为空");
        }
        FitMemberCard db = requireCard(cardId);
        if (Objects.equals(db.getCardStatus(), cardStatus)) {
            return true;
        }
        FitMemberCard update = new FitMemberCard();
        update.setCardId(cardId);
        update.setCardStatus(cardStatus);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Lock4j(keys = {"'card:adjust:' + #cardId"}, acquireTimeout = 3000)
    public Boolean adjustRights(Long cardId, FitnessMemberCardBo bo) {
        FitMemberCard db = requireCard(cardId);
        int finalTotalTimes = defaultInt(db.getTotalTimes()) + defaultInt(bo.getTotalTimesAdjust());
        int finalRemainTimes = defaultInt(db.getRemainTimes()) + defaultInt(bo.getRemainTimesAdjust());
        BigDecimal finalBalance = defaultDecimal(db.getBalanceAmount()).add(defaultDecimal(bo.getBalanceAmountAdjust()));

        if (finalTotalTimes < 0) {
            throw new ServiceException("总次数不能调整为负数");
        }
        if (finalRemainTimes < 0) {
            throw new ServiceException("剩余次数不能调整为负数");
        }
        if (finalBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("剩余余额不能调整为负数");
        }
        if (finalTotalTimes > 0 && defaultInt(db.getUsedTimes()) + finalRemainTimes > finalTotalTimes) {
            throw new ServiceException("调整后总次数不能小于已用次数与剩余次数之和");
        }

        FitMemberCard update = new FitMemberCard();
        update.setCardId(cardId);
        if (bo.getBindCoachId() != null) {
            update.setBindCoachId(bo.getBindCoachId());
        }
        if (bo.getExpireTime() != null) {
            update.setExpireTime(bo.getExpireTime());
        }
        if (bo.getTotalTimesAdjust() != null) {
            update.setTotalTimes(finalTotalTimes);
        }
        if (bo.getRemainTimesAdjust() != null) {
            update.setRemainTimes(finalRemainTimes);
        }
        if (bo.getBalanceAmountAdjust() != null) {
            update.setBalanceAmount(finalBalance);
        }
        if (StringUtils.isNotBlank(bo.getRemark())) {
            update.setRemark(bo.getRemark());
        }

        String nextStatus = StringUtils.defaultIfBlank(bo.getCardStatus(), db.getCardStatus());
        if (StringUtils.isBlank(bo.getCardStatus())) {
            nextStatus = resolveStatusByRights(nextStatus, bo.getExpireTime() == null ? db.getExpireTime() : bo.getExpireTime(), finalTotalTimes, finalRemainTimes);
        }
        update.setCardStatus(nextStatus);
        return baseMapper.updateById(update) > 0;
    }

    private FitMemberCard requireCard(Long cardId) {
        FitMemberCard card = baseMapper.selectById(cardId);
        if (card == null) {
            throw new ServiceException("会员卡不存在");
        }
        return card;
    }

    private String resolveStatusByRights(String currentStatus, Date expireTime, int totalTimes, int remainTimes) {
        if (Objects.equals(currentStatus, "frozen")) {
            return currentStatus;
        }
        if (expireTime != null && expireTime.before(new Date())) {
            return "expired";
        }
        if (totalTimes > 0 && remainTimes <= 0) {
            return "exhausted";
        }
        if (Objects.equals(currentStatus, "expired") || Objects.equals(currentStatus, "exhausted")) {
            return "active";
        }
        return currentStatus;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
