package org.dromara.fitness.service.impl;

import com.baomidou.lock.annotation.Lock4j;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.fitness.domain.FitCheckinRecord;
import org.dromara.fitness.domain.FitCourseReservation;
import org.dromara.fitness.domain.FitMemberCard;
import org.dromara.fitness.domain.bo.FitnessCheckinBo;
import org.dromara.fitness.domain.vo.FitnessCheckinVo;
import org.dromara.fitness.mapper.FitnessCheckinMapper;
import org.dromara.fitness.mapper.FitnessMemberCardMapper;
import org.dromara.fitness.mapper.FitnessReservationMapper;
import org.dromara.fitness.mapper.FitnessScheduleMapper;
import org.dromara.fitness.service.IFitnessCheckinService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 签到核销 Service 实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessCheckinServiceImpl implements IFitnessCheckinService {

    private final FitnessCheckinMapper baseMapper;
    private final FitnessScheduleMapper scheduleMapper;
    private final FitnessMemberCardMapper memberCardMapper;
    private final FitnessReservationMapper reservationMapper;

    @Override
    public FitnessCheckinVo queryById(Long checkinId) {
        FitnessCheckinVo vo = baseMapper.selectCheckinById(checkinId);
        if (vo == null) {
            throw new ServiceException("签到记录不存在");
        }
        return vo;
    }

    @Override
    public TableDataInfo<FitnessCheckinVo> queryPageList(FitnessCheckinBo bo, PageQuery pageQuery) {
        return TableDataInfo.build(baseMapper.selectPageCheckinList(pageQuery.build(), bo));
    }

    @Override
    public List<FitnessCheckinVo> queryList(FitnessCheckinBo bo) {
        return baseMapper.selectCheckinList(bo);
    }

    @Override
    @Lock4j(keys = {"'checkin:confirm:' + #checkinId"}, acquireTimeout = 3000)
    @Transactional(rollbackFor = Exception.class)
    public Boolean confirmCheckin(Long checkinId, FitnessCheckinBo bo) {
        FitnessCheckinVo actionVo = queryById(checkinId);
        if (Objects.equals("reversed", actionVo.getCheckinStatus())) {
            throw new ServiceException("该签到已标记异常，不能确认核销");
        }
        if (actionVo.getOperatorUserId() != null) {
            throw new ServiceException("该签到已确认核销");
        }
        int consumeTimes = bo.getConsumeTimes() == null
            ? (actionVo.getCardId() == null || Objects.equals("time_card", actionVo.getPlanType()) ? 0 : 1)
            : bo.getConsumeTimes();

        BigDecimal consumeAmount = bo.getConsumeAmount() == null ? BigDecimal.ZERO : bo.getConsumeAmount();
        validateCardConsume(actionVo, consumeTimes, consumeAmount);

        FitCheckinRecord update = new FitCheckinRecord();
        update.setCheckinId(checkinId);
        update.setOperatorUserId(LoginHelper.getUserId());
        update.setConsumeTimes(consumeTimes);
        update.setConsumeAmount(consumeAmount);
        update.setCheckinStatus("success");
        update.setRemark(bo.getRemark());
        if (baseMapper.updateById(update) <= 0) {
            throw new ServiceException("核销失败，请重试");
        }
        scheduleMapper.updateSignedCount(actionVo.getScheduleId(), 1);
        if (actionVo.getCardId() != null && (consumeTimes > 0 || consumeAmount.compareTo(BigDecimal.ZERO) > 0)) {
            consumeMemberCard(actionVo, consumeTimes, consumeAmount);
        }
        if (actionVo.getReservationId() != null) {
            FitCourseReservation reservation = new FitCourseReservation();
            reservation.setReservationId(actionVo.getReservationId());
            reservation.setReservationStatus("signed");
            reservationMapper.updateById(reservation);
        }
        return true;
    }

    @Override
    @Lock4j(keys = {"'checkin:exception:' + #checkinId"}, acquireTimeout = 3000)
    public Boolean markCheckinException(Long checkinId, String remark) {
        FitnessCheckinVo actionVo = queryById(checkinId);
        if (actionVo.getOperatorUserId() != null && Objects.equals("success", actionVo.getCheckinStatus())) {
            throw new ServiceException("该签到已确认，不能再标记异常");
        }
        FitCheckinRecord update = new FitCheckinRecord();
        update.setCheckinId(checkinId);
        update.setOperatorUserId(LoginHelper.getUserId());
        update.setCheckinStatus("reversed");
        update.setRemark(remark);
        return baseMapper.updateById(update) > 0;
    }

    private void validateCardConsume(FitnessCheckinVo actionVo, int consumeTimes, BigDecimal consumeAmount) {
        if (actionVo.getCardId() == null) {
            return;
        }
        if (!Objects.equals("unused", actionVo.getCardStatus()) && !Objects.equals("active", actionVo.getCardStatus())) {
            throw new ServiceException("会员卡状态不可核销");
        }
        if (consumeTimes > 0 && actionVo.getRemainTimes() != null && consumeTimes > actionVo.getRemainTimes()) {
            throw new ServiceException("剩余课次不足");
        }
        if (consumeAmount.compareTo(BigDecimal.ZERO) > 0 && actionVo.getBalanceAmount() != null
            && consumeAmount.compareTo(actionVo.getBalanceAmount()) > 0) {
            throw new ServiceException("余额不足，无法完成核销");
        }
    }

    private void consumeMemberCard(FitnessCheckinVo actionVo, int consumeTimes, BigDecimal consumeAmount) {
        FitMemberCard card = memberCardMapper.selectById(actionVo.getCardId());
        if (card == null) {
            throw new ServiceException("会员卡不存在");
        }
        int usedTimes = defaultInt(card.getUsedTimes()) + Math.max(consumeTimes, 0);
        int remainTimes = Math.max(defaultInt(card.getRemainTimes()) - Math.max(consumeTimes, 0), 0);
        BigDecimal balanceAmount = defaultDecimal(card.getBalanceAmount()).subtract(consumeAmount == null ? BigDecimal.ZERO : consumeAmount);
        if (balanceAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("会员卡余额不足");
        }

        FitMemberCard update = new FitMemberCard();
        update.setCardId(card.getCardId());
        update.setUsedTimes(usedTimes);
        update.setRemainTimes(remainTimes);
        update.setBalanceAmount(balanceAmount);
        update.setCardStatus(resolveCardStatus(card.getCardStatus(), card.getTotalTimes(), remainTimes));
        if (memberCardMapper.updateById(update) <= 0) {
            throw new ServiceException("更新会员卡权益失败");
        }
    }

    private String resolveCardStatus(String currentStatus, Integer totalTimes, Integer remainTimes) {
        if (StringUtils.equalsAny(currentStatus, "frozen", "expired")) {
            return currentStatus;
        }
        if (defaultInt(totalTimes) > 0 && defaultInt(remainTimes) <= 0) {
            return "exhausted";
        }
        if (Objects.equals("unused", currentStatus) || Objects.equals("exhausted", currentStatus)) {
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
