package org.dromara.fitness.service.impl;

import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.domain.FitCourseReservation;
import org.dromara.fitness.domain.FitMember;
import org.dromara.fitness.domain.FitMemberCard;

import org.dromara.fitness.domain.bo.FitnessReservationBo;
import org.dromara.fitness.domain.vo.FitnessReservationVo;
import org.dromara.fitness.domain.vo.FitnessScheduleVo;
import org.dromara.fitness.mapper.FitnessMemberCardMapper;
import org.dromara.fitness.mapper.FitnessMemberMapper;
import org.dromara.fitness.mapper.FitnessReservationMapper;

import org.dromara.fitness.mapper.FitnessScheduleMapper;
import org.dromara.fitness.service.IFitnessReservationService;
import org.dromara.fitness.utils.FitnessBizCodeUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 预约 Service 实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessReservationServiceImpl implements IFitnessReservationService {

    private final FitnessReservationMapper baseMapper;
    private final FitnessScheduleMapper scheduleMapper;
    private final FitnessMemberMapper memberMapper;
    private final FitnessMemberCardMapper memberCardMapper;


    @Override
    public FitnessReservationVo queryById(Long reservationId) {
        return baseMapper.selectReservationById(reservationId);
    }

    @Override
    public TableDataInfo<FitnessReservationVo> queryPageList(FitnessReservationBo bo, PageQuery pageQuery) {
        return TableDataInfo.build(baseMapper.selectPageReservationList(pageQuery.build(), bo));
    }

    @Override
    public List<FitnessReservationVo> queryList(FitnessReservationBo bo) {
        return baseMapper.selectReservationList(bo);
    }

    @Override
    @Lock4j(keys = {"'reservation:create:' + #bo.scheduleId + ':' + #bo.memberId"}, acquireTimeout = 3000)
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(FitnessReservationBo bo) {
        FitnessScheduleVo scheduleVo = validBeforeCreate(bo);
        ReservationRule reservationRule = resolveReservationRule(bo, scheduleVo);
        FitCourseReservation add = MapstructUtils.convert(bo, FitCourseReservation.class);
        add.setReservationId(IdGeneratorUtil.nextLongId());
        add.setReservationNo(FitnessBizCodeUtils.defaultIfBlank(bo.getReservationNo(), "RSV"));
        validReservationNoUnique(add.getReservationNo(), null);
        add.setTenantId(currentTenantId());
        add.setStoreId(add.getStoreId() == null ? scheduleVo.getStoreId() : add.getStoreId());
        add.setCardId(reservationRule.cardId());
        add.setReservationType(reservationRule.reservationType());
        add.setReservationSource(StringUtils.defaultIfBlank(bo.getReservationSource(), "admin"));
        add.setReservationStatus(StringUtils.defaultIfBlank(bo.getReservationStatus(), "reserved"));
        add.setConsumeMode(StringUtils.defaultIfBlank(bo.getConsumeMode(), reservationRule.consumeMode()));
        add.setReservationTime(bo.getReservationTime() == null ? new Date() : bo.getReservationTime());
        if (scheduleMapper.updateReservedCount(add.getScheduleId(), 1) <= 0) {
            throw new ServiceException("预约失败，排课人数更新异常");
        }
        boolean flag = baseMapper.insert(add) > 0;

        if (!flag) {
            scheduleMapper.updateReservedCount(add.getScheduleId(), -1);
            throw new ServiceException("预约失败");
        }
        bo.setReservationId(add.getReservationId());
        return true;
    }


    @Override
    public Boolean updateByBo(FitnessReservationBo bo) {
        FitCourseReservation db = baseMapper.selectById(bo.getReservationId());
        if (db == null) {
            throw new ServiceException("预约记录不存在");
        }
        if (StringUtils.equalsAny(db.getReservationStatus(), "signed", "absent", "leave")) {
            throw new ServiceException("当前预约状态不允许编辑");
        }
        if (Objects.equals("cancelled", bo.getReservationStatus())) {
            return cancelReservation(bo.getReservationId(), bo.getCancelReason());
        }
        if ((bo.getScheduleId() != null && !Objects.equals(bo.getScheduleId(), db.getScheduleId()))
            || (bo.getMemberId() != null && !Objects.equals(bo.getMemberId(), db.getMemberId()))) {
            throw new ServiceException("不支持直接修改排课或会员，请取消后重新创建预约");
        }
        FitCourseReservation update = MapstructUtils.convert(bo, FitCourseReservation.class);
        update.setReservationNo(FitnessBizCodeUtils.fillOrKeep(bo.getReservationNo(), db.getReservationNo(), "RSV"));
        validReservationNoUnique(update.getReservationNo(), bo.getReservationId());
        return baseMapper.updateById(update) > 0;
    }


    @Override
    @Lock4j(keys = {"'reservation:cancel:' + #reservationId"}, acquireTimeout = 3000)
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelReservation(Long reservationId, String cancelReason) {
        FitCourseReservation db = baseMapper.selectById(reservationId);
        if (db == null) {
            throw new ServiceException("预约记录不存在");
        }
        if (Objects.equals("cancelled", db.getReservationStatus())) {
            return true;
        }
        if (!Objects.equals("reserved", db.getReservationStatus())) {
            throw new ServiceException("当前预约状态不可取消");
        }
        FitCourseReservation update = new FitCourseReservation();
        update.setReservationId(reservationId);
        update.setReservationStatus("cancelled");
        update.setCancelReason(StringUtils.defaultIfBlank(cancelReason, "admin_cancel"));
        update.setCancelTime(new Date());
        boolean flag = baseMapper.updateById(update) > 0;
        if (flag) {
            scheduleMapper.updateReservedCount(db.getScheduleId(), -1);
        }
        return flag;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            long count = baseMapper.selectCount(new LambdaQueryWrapper<FitCourseReservation>().in(FitCourseReservation::getReservationId, ids));
            if (count != ids.size()) {
                throw new ServiceException("部分预约记录不存在或已被删除");
            }
            long reservedCount = baseMapper.selectCount(new LambdaQueryWrapper<FitCourseReservation>()
                .in(FitCourseReservation::getReservationId, ids)
                .eq(FitCourseReservation::getReservationStatus, "reserved"));
            if (reservedCount > 0) {
                throw new ServiceException("存在待上课预约，需先取消后再删除");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    private FitnessScheduleVo validBeforeCreate(FitnessReservationBo bo) {
        long memberCount = memberMapper.selectCount(new LambdaQueryWrapper<FitMember>().eq(FitMember::getMemberId, bo.getMemberId()));
        if (memberCount <= 0) {
            throw new ServiceException("会员不存在");
        }
        FitnessScheduleVo scheduleVo = scheduleMapper.selectScheduleById(bo.getScheduleId());
        if (scheduleVo == null) {
            throw new ServiceException("排课不存在");
        }
        if (StringUtils.equalsAny(scheduleVo.getScheduleStatus(), "cancelled", "finished", "closed")) {
            throw new ServiceException("当前排课状态不支持预约");
        }
        Date now = new Date();
        if (scheduleVo.getReservationDeadline() != null && now.after(scheduleVo.getReservationDeadline())) {
            throw new ServiceException("已超过预约截止时间");
        }
        if (scheduleVo.getStartTime() != null && now.after(scheduleVo.getStartTime())) {
            throw new ServiceException("课程已开始，无法新增预约");
        }
        if (scheduleVo.getCapacity() != null && scheduleVo.getReservedCount() != null
            && scheduleVo.getReservedCount() >= scheduleVo.getCapacity()) {
            throw new ServiceException("当前排课预约已满");
        }
        long activeCount = baseMapper.selectCount(new LambdaQueryWrapper<FitCourseReservation>()
            .eq(FitCourseReservation::getMemberId, bo.getMemberId())
            .eq(FitCourseReservation::getScheduleId, bo.getScheduleId())
            .eq(FitCourseReservation::getReservationStatus, "reserved"));
        if (activeCount > 0) {
            throw new ServiceException("该会员已预约当前排课");
        }
        return scheduleVo;
    }

    private void validReservationNoUnique(String reservationNo, Long excludeReservationId) {
        long count = baseMapper.selectCount(new LambdaQueryWrapper<FitCourseReservation>()
            .eq(FitCourseReservation::getReservationNo, reservationNo)
            .ne(excludeReservationId != null, FitCourseReservation::getReservationId, excludeReservationId));
        if (count > 0) {
            throw new ServiceException("预约编号已存在");
        }
    }

    private ReservationRule resolveReservationRule(FitnessReservationBo bo, FitnessScheduleVo scheduleVo) {
        Long cardId = bo.getCardId();
        if (cardId == null) {
            if (isPrivateOrSmall(scheduleVo.getCourseType())) {
                throw new ServiceException("私教/小班课程预约需选择对应卡项");
            }
            return new ReservationRule(null, "store_experience", "free");
        }
        FitMemberCard card = memberCardMapper.selectById(cardId);
        if (card == null || !Objects.equals(card.getMemberId(), bo.getMemberId())) {
            throw new ServiceException("会员卡不存在或不属于当前会员");
        }
        if (!StringUtils.equalsAny(card.getCardStatus(), "unused", "active")) {
            throw new ServiceException("会员卡当前状态不可预约");
        }
        if (StringUtils.equals(card.getPlanType(), "time_card")) {
            throw new ServiceException("时间卡（月卡/年卡）无需预约和核销，请直接到店上课");
        }
        if (isPrivateOrSmall(scheduleVo.getCourseType())
            && card.getBindCoachId() != null
            && !Objects.equals(card.getBindCoachId(), scheduleVo.getCoachId())) {
            throw new ServiceException("当前预约教练与私教卡绑定教练不一致");
        }
        String consumeMode = StringUtils.equals(card.getPlanType(), "stored_card") ? "amount" : "times";
        return new ReservationRule(cardId, "card", consumeMode);
    }

    private boolean isPrivateOrSmall(String courseType) {
        return Objects.equals("private", courseType) || Objects.equals("small", courseType);
    }

    private record ReservationRule(Long cardId, String reservationType, String consumeMode) {
    }

    private String currentTenantId() {

        return StringUtils.defaultIfBlank(TenantHelper.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
    }
}

