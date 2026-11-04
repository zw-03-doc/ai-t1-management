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
import org.dromara.fitness.service.support.FitnessOperationsService;
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

    private final FitnessOperationsService operations;
    private final org.dromara.fitness.service.support.FitnessEntitlementService entitlements;
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
        FitnessCheckinVo initial = queryById(checkinId);
        FitCourseReservation locked = operations.lockReservation(initial.getReservationId());
        if (!"reserved".equals(locked.getReservationStatus())) throw new ServiceException("当前预约已处理，不能重复核销");
        FitnessCheckinVo actionVo = queryById(checkinId);
        if (Objects.equals("reversed", actionVo.getCheckinStatus())) {
            throw new ServiceException("该签到已标记异常，不能确认核销");
        }
        if (actionVo.getOperatorUserId() != null) {
            throw new ServiceException("该签到已确认核销");
        }
        var usage = entitlements.consume(locked, bo.getConsumeTimes(), bo.getConsumeAmount());
        operations.confirmRecord(checkinId, LoginHelper.getUserId(), usage.times(), usage.amount(), bo.getRemark());
        if (scheduleMapper.updateSignedCount(actionVo.getScheduleId(), 1) != 1) throw new ServiceException("场次状态已变化，请刷新后重试");
        locked.setReservationStatus("signed");
        if (reservationMapper.updateById(locked) != 1) throw new ServiceException("预约状态已变化，请刷新后重试");
        operations.audit(actionVo.getMemberId(), actionVo.getStoreId(), actionVo.getScheduleId(), actionVo.getReservationId(), checkinId, "admin_confirm_checkin", "web", bo.getRemark());
        return true;
    }

    @Override
    @Lock4j(keys = {"'checkin:exception:' + #checkinId"}, acquireTimeout = 3000)
    @Transactional(rollbackFor = Exception.class)
    public Boolean markCheckinException(Long checkinId, String remark) {
        FitnessCheckinVo initial = queryById(checkinId);
        operations.lockReservation(initial.getReservationId());
        FitnessCheckinVo actionVo = queryById(checkinId);
        if (actionVo.getOperatorUserId() != null && Objects.equals("success", actionVo.getCheckinStatus())) {
            throw new ServiceException("该签到已确认，不能再标记异常");
        }
        operations.markException(checkinId, LoginHelper.getUserId(), remark);
        operations.audit(actionVo.getMemberId(), actionVo.getStoreId(), actionVo.getScheduleId(), actionVo.getReservationId(), checkinId, "admin_checkin_exception", "web", remark);
        return true;
    }

}
