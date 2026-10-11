package org.dromara.fitness.service.impl;

import com.baomidou.lock.annotation.Lock4j;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.FitMemberCard;
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
