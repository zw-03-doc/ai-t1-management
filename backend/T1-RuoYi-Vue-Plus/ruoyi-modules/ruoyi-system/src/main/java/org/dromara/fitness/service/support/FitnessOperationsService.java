package org.dromara.fitness.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.fitness.domain.*;
import org.dromara.fitness.domain.bo.FitnessEngagementBo;
import org.dromara.fitness.domain.bo.FitnessScheduleBo;
import org.dromara.fitness.domain.model.FitnessLoginUser;
import org.dromara.fitness.domain.vo.FitnessEngagementVo;
import org.dromara.fitness.mapper.*;
import org.dromara.fitness.utils.FitnessBizCodeUtils;
import org.dromara.fitness.utils.FitnessBookingPolicy;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FitnessOperationsService {
    private final FitnessOperationsMapper mapper;
    private final FitnessCourseMapper courseMapper;
    private final FitnessCoachMapper coachMapper;
    private final FitnessStoreMapper storeMapper;
    private final FitnessScheduleMapper scheduleMapper;
    private final FitnessReservationMapper reservationMapper;
    private final FitnessMemberMapper memberMapper;
    private final FitnessCheckinMapper checkinMapper;

    public String tenant() { return FitnessSecurityUtils.getCurrentTenantId(); }

    public void prepareCourse(FitCourse c, FitCourse old) {
        FitStore store = c.getStoreId() == null ? null : storeMapper.selectById(c.getStoreId());
        if (store == null || !Objects.equals(store.getTenantId(), tenant()) || !"0".equals(store.getStatus()))
            throw new ServiceException("请选择有效门店");
        if (old != null && !Objects.equals(c.getStoreId(), old.getStoreId()) && scheduleMapper.selectCount(
            new LambdaQueryWrapper<FitCourseSchedule>().eq(FitCourseSchedule::getCourseId, old.getCourseId())) > 0)
            throw new ServiceException("已有排课的课程不能直接变更门店，请为新门店新建课程");
        if (c.getDefaultCoachId() != null) requireCoach(c.getDefaultCoachId(), c.getStoreId(), false);
        c.setCapacity(FitnessBookingPolicy.range(c.getCapacity(), old == null || old.getCapacity() == null ? 20 : old.getCapacity(), 1, 1000, "人数上限"));
        c.setDurationMinutes(FitnessBookingPolicy.range(c.getDurationMinutes(), 60, 1, 1440, "课程分钟数"));
        c.setDisplayPrice(FitnessBookingPolicy.price(c.getDisplayPrice() == null && old != null ? old.getDisplayPrice() : c.getDisplayPrice()));
        c.setListingDays(FitnessBookingPolicy.range(c.getListingDays(), old == null || old.getListingDays() == null ? 30 : old.getListingDays(), 1, 365, "上架天数"));
        c.setBookingAdvanceDays(FitnessBookingPolicy.range(c.getBookingAdvanceDays(), old == null || old.getBookingAdvanceDays() == null ? 7 : old.getBookingAdvanceDays(), 0, 365, "提前预约天数"));
        c.setBookingCloseMinutes(FitnessBookingPolicy.range(c.getBookingCloseMinutes(), old == null || old.getBookingCloseMinutes() == null ? 30 : old.getBookingCloseMinutes(), 0, 10080, "预约截止提前分钟数"));
        c.setCancelCloseMinutes(FitnessBookingPolicy.range(c.getCancelCloseMinutes(), old == null || old.getCancelCloseMinutes() == null ? 60 : old.getCancelCloseMinutes(), 0, 10080, "取消截止提前分钟数"));
        c.setCheckinBeforeMinutes(FitnessBookingPolicy.range(c.getCheckinBeforeMinutes(), old == null || old.getCheckinBeforeMinutes() == null ? 30 : old.getCheckinBeforeMinutes(), 0, 1440, "签到提前分钟数"));
        c.setCheckinAfterMinutes(FitnessBookingPolicy.range(c.getCheckinAfterMinutes(), old == null || old.getCheckinAfterMinutes() == null ? 60 : old.getCheckinAfterMinutes(), 0, 1440, "课后签到分钟数"));
        if (c.getPublishedAt() == null) c.setPublishedAt(old == null || old.getPublishedAt() == null ? new Date() : old.getPublishedAt());
        c.setListingUntil(FitnessBookingPolicy.daysBefore(c.getPublishedAt(), -c.getListingDays()));
        if (c.getStatus() == null) c.setStatus(old == null ? "1" : old.getStatus());
        if (!Set.of("0", "1").contains(c.getStatus())) throw new ServiceException("课程状态不正确");
    }

    public void prepareSchedule(FitnessScheduleBo bo) {
        FitnessBookingPolicy.validateScheduleDate(bo.getScheduleDate(), bo.getStartTime(), bo.getEndTime());
        FitCourse c = courseMapper.selectById(bo.getCourseId());
        if (c == null || !Objects.equals(c.getTenantId(), tenant())) throw new ServiceException("课程不存在");
        if (bo.getStoreId() == null) bo.setStoreId(c.getStoreId());
        if (!Objects.equals(bo.getStoreId(), c.getStoreId())) throw new ServiceException("排课门店必须与课程门店一致");
        requireActiveStore(bo.getStoreId());
        if (bo.getCoachId() == null) bo.setCoachId(c.getDefaultCoachId());
        requireCoach(bo.getCoachId(), bo.getStoreId(), true);
        if (mapper.countCoachConflicts(tenant(), bo.getCoachId(), bo.getScheduleId(), bo.getStartTime(), bo.getEndTime()) > 0)
            throw new ServiceException("该教练在此时间段已有排课");
        bo.setCapacity(FitnessBookingPolicy.range(bo.getCapacity(), c.getCapacity() == null ? 20 : c.getCapacity(), 1, 1000, "场次名额"));
        bo.setDisplayPrice(FitnessBookingPolicy.price(bo.getDisplayPrice() == null ? c.getDisplayPrice() : bo.getDisplayPrice()));
        if (bo.getReservationOpenTime() == null) bo.setReservationOpenTime(FitnessBookingPolicy.bookingOpensAt(bo.getStartTime(), c.getBookingAdvanceDays() == null ? 7 : c.getBookingAdvanceDays()));
        if (bo.getReservationDeadline() == null) bo.setReservationDeadline(FitnessBookingPolicy.minutesBefore(bo.getStartTime(), c.getBookingCloseMinutes() == null ? 30 : c.getBookingCloseMinutes()));
        if (bo.getCancelDeadline() == null) bo.setCancelDeadline(FitnessBookingPolicy.minutesBefore(bo.getStartTime(), c.getCancelCloseMinutes() == null ? 60 : c.getCancelCloseMinutes()));
        if (!bo.getReservationDeadline().after(bo.getReservationOpenTime()) || bo.getReservationDeadline().after(bo.getStartTime()) || bo.getCancelDeadline().after(bo.getStartTime()))
            throw new ServiceException("预约开放时间须早于截止时间，预约/取消截止时间不得晚于开课时间");
        if (bo.getScheduleStatus() != null && !Set.of("draft", "open", "closed", "cancelled", "finished").contains(bo.getScheduleStatus()))
            throw new ServiceException("排课状态不正确");
        FitnessBookingPolicy.validateAudience(bo.getScheduleMode(), bo.getStudentMemberIds(), bo.getCapacity());
        if (bo.getStudentMemberIds() != null) {
            for (Long memberId : bo.getStudentMemberIds()) {
                FitMember member = memberMapper.selectOne(new LambdaQueryWrapper<FitMember>().eq(FitMember::getMemberId, memberId)
                    .eq(FitMember::getTenantId, tenant()).last("FOR UPDATE"));
                if (member == null || !"normal".equals(member.getMemberStatus())) throw new ServiceException("所选学员不存在或已冻结");
            }
            if (bo.getStudentMemberIds().stream().distinct().count() > bo.getCapacity()) throw new ServiceException("定向学员数量不能超过场次名额");
        }
    }

    private FitCoach requireCoach(Long id, Long storeId, boolean lock) {
        if (id == null) throw new ServiceException("请选择授课教练");
        LambdaQueryWrapper<FitCoach> q = new LambdaQueryWrapper<FitCoach>().eq(FitCoach::getCoachId, id).eq(FitCoach::getTenantId, tenant());
        if (lock) q.last("FOR UPDATE");
        FitCoach coach = coachMapper.selectOne(q);
        if (coach == null || !Objects.equals(coach.getStoreId(), storeId) || !Objects.equals(coach.getHireStatus(), "0"))
            throw new ServiceException("教练须属于所选门店且处于在职状态");
        return coach;
    }

    private void requireActiveStore(Long id) {
        FitStore store = id == null ? null : storeMapper.selectById(id);
        if (store == null || !Objects.equals(store.getTenantId(), tenant()) || !"0".equals(store.getStatus()))
            throw new ServiceException("门店已停用或不存在");
    }

    /** Call only from a transactional operation: one schedule row serializes competing bookings. */
    public FitCourseSchedule requireReservationWindow(Long scheduleId, Long memberId) {
        FitCourseSchedule s = lockSchedule(scheduleId);
        FitMember member = memberMapper.selectOne(new LambdaQueryWrapper<FitMember>().eq(FitMember::getMemberId, memberId)
            .eq(FitMember::getTenantId, tenant()).last("FOR UPDATE"));
        if (member == null || !"normal".equals(member.getMemberStatus())) throw new ServiceException("会员不存在或已冻结");
        FitCourse c = courseMapper.selectById(s.getCourseId());
        if (!"open".equals(s.getScheduleStatus()) || c == null || !"0".equals(c.getStatus())) throw new ServiceException("课程尚未开放预约或已下架");
        requireActiveStore(s.getStoreId());
        requireCoach(s.getCoachId(), s.getStoreId(), false);
        Date now = new Date();
        FitnessBookingPolicy.validateWindow(now, c.getPublishedAt(), c.getListingUntil(), "课程上架");
        FitnessBookingPolicy.validateWindow(now, s.getReservationOpenTime(), s.getReservationDeadline(), "预约");
        if (s.getStartTime() == null || !now.before(s.getStartTime())) throw new ServiceException("课程已开始");
        if (s.getCapacity() == null || s.getReservedCount() != null && s.getReservedCount() >= s.getCapacity()) throw new ServiceException("预约名额已满");
        var assigned = scheduleMapper.selectScheduleStudentIds(scheduleId);
        if (!"normal".equals(s.getScheduleMode()) && (assigned == null || !assigned.contains(memberId)))
            throw new ServiceException("此场次仅限已指定学员预约");
        long count = reservationMapper.selectCount(new LambdaQueryWrapper<FitCourseReservation>()
            .eq(FitCourseReservation::getScheduleId, scheduleId).eq(FitCourseReservation::getMemberId, memberId)
            .in(FitCourseReservation::getReservationStatus, "reserved", "signed"));
        if (count > 0) throw new ServiceException("您已预约或签到此场次");
        return s;
    }

    public FitCourseSchedule lockSchedule(Long id) {
        FitCourseSchedule s = scheduleMapper.selectOne(new LambdaQueryWrapper<FitCourseSchedule>()
            .eq(FitCourseSchedule::getScheduleId, id).eq(FitCourseSchedule::getTenantId, tenant()).last("FOR UPDATE"));
        if (s == null) throw new ServiceException("排课不存在");
        return s;
    }

    public FitCourseReservation lockReservation(Long id) {
        FitCourseReservation r = reservationMapper.selectOne(new LambdaQueryWrapper<FitCourseReservation>()
            .eq(FitCourseReservation::getReservationId, id).eq(FitCourseReservation::getTenantId, tenant()).last("FOR UPDATE"));
        if (r == null) throw new ServiceException("预约不存在");
        return r;
    }

    public void requireCheckinWindow(Long scheduleId) {
        FitCourseSchedule s = scheduleMapper.selectById(scheduleId);
        if (s == null || !Objects.equals(s.getTenantId(), tenant()) || Set.of("draft", "cancelled", "finished").contains(s.getScheduleStatus()))
            throw new ServiceException("此场次无法签到");
        FitCourse c = courseMapper.selectById(s.getCourseId());
        if (c == null) throw new ServiceException("课程不存在");
        FitnessBookingPolicy.validateWindow(new Date(),
            FitnessBookingPolicy.minutesBefore(s.getStartTime(), c.getCheckinBeforeMinutes() == null ? 30 : c.getCheckinBeforeMinutes()),
            FitnessBookingPolicy.minutesBefore(s.getEndTime(), -(c.getCheckinAfterMinutes() == null ? 60 : c.getCheckinAfterMinutes())), "签到");
    }

    public void requireNoCheckin(Long reservationId) {
        if (checkinMapper.selectCount(new LambdaQueryWrapper<FitCheckinRecord>()
            .eq(FitCheckinRecord::getReservationId, reservationId)) > 0)
            throw new ServiceException("已经签到的预约不能取消");
    }

    public void requireNoScheduleHistory(Long scheduleId) {
        if (reservationMapper.selectCount(new LambdaQueryWrapper<FitCourseReservation>()
            .eq(FitCourseReservation::getScheduleId, scheduleId)) > 0)
            throw new ServiceException("此场次已有预约历史，请保留以便查询行为记录");
    }

    public void requireScheduleCanFinish(FitCourseSchedule schedule) {
        if (Set.of("draft", "cancelled").contains(Objects.toString(schedule.getScheduleStatus(), ""))) throw new ServiceException("草稿或已取消场次不能结课");
        if (schedule.getEndTime() == null || new Date().before(schedule.getEndTime())) throw new ServiceException("场次尚未结束，不能提前结课");
        long pending = reservationMapper.selectCount(new LambdaQueryWrapper<FitCourseReservation>()
            .eq(FitCourseReservation::getScheduleId, schedule.getScheduleId()).eq(FitCourseReservation::getReservationStatus, "reserved"));
        if (pending > 0) throw new ServiceException("还有" + pending + "条预约待处理，请到预约管理完成核销或登记缺席/请假");
    }

    @Transactional(rollbackFor = Exception.class)
    public void resolveReservation(Long reservationId, String status, String reason, Long coachId) {
        if (!Set.of("absent", "leave").contains(Objects.toString(status, ""))) throw new ServiceException("请选择缺席或请假");
        requireReason(reason);
        FitCourseReservation r = lockReservation(reservationId);
        FitCourseSchedule s = scheduleMapper.selectById(r.getScheduleId());
        if (s == null || "finished".equals(s.getScheduleStatus())) throw new ServiceException("场次已结束或不存在");
        if (coachId != null) requireScheduleCoach(s, coachId);
        if (!"reserved".equals(r.getReservationStatus())) throw new ServiceException("只有待处理预约可登记缺席或请假");
        if ("absent".equals(status) && new Date().before(s.getEndTime())) throw new ServiceException("课程结束后才能登记缺席");
        FitCheckinRecord checkin = checkinMapper.selectOne(new LambdaQueryWrapper<FitCheckinRecord>().eq(FitCheckinRecord::getReservationId, reservationId));
        if (checkin != null && !"reversed".equals(checkin.getCheckinStatus())) throw new ServiceException("已有到场签到，请先核销或审核标记异常");
        r.setReservationStatus(status); r.setRemark(reason);
        if (reservationMapper.updateById(r) != 1) throw new ServiceException("预约已变化，请刷新后重试");
        boolean releaseSeat = "leave".equals(status) && s.getStartTime() != null && new Date().before(s.getStartTime());
        if (releaseSeat && scheduleMapper.updateReservedCount(s.getScheduleId(), -1) != 1)
            throw new ServiceException("预约名额状态已变化，请刷新后重试");
        audit(r.getMemberId(), r.getStoreId(), r.getScheduleId(), r.getReservationId(), checkin == null ? null : checkin.getCheckinId(),
            coachId == null ? "admin_resolve_reservation" : "coach_resolve_reservation", "web", status + ": " + reason + (releaseSeat ? "；开课前请假，已释放1个名额" : ""));
    }

    @Transactional(rollbackFor = Exception.class)
    public void restoreCheckin(Long checkinId, String reason, Long coachId) {
        requireReason(reason);
        FitCheckinRecord initial = checkinMapper.selectById(checkinId);
        if (initial == null) throw new ServiceException("签到记录不存在");
        FitCourseReservation r = lockReservation(initial.getReservationId());
        FitCourseSchedule s = scheduleMapper.selectById(r.getScheduleId());
        if (s == null || "finished".equals(s.getScheduleStatus()) || !"reserved".equals(r.getReservationStatus()))
            throw new ServiceException("仅未结束场次的待处理预约可以恢复签到");
        if (coachId != null) requireScheduleCoach(s, coachId);
        if (mapper.restoreCheckin(checkinId, tenant(), reason) != 1) throw new ServiceException("仅异常签到可恢复，请刷新后重试");
        audit(r.getMemberId(), r.getStoreId(), r.getScheduleId(), r.getReservationId(), checkinId,
            coachId == null ? "admin_restore_checkin" : "coach_restore_checkin", "web", reason);
    }

    public void requireScheduleCoach(FitCourseSchedule schedule, Long coachId) {
        if (!Objects.equals(schedule.getCoachId(), coachId)) throw new ServiceException("只能处理自己授课场次的预约");
        requireCoach(coachId, schedule.getStoreId(), false);
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 500) throw new ServiceException("请填写500字以内的处理原因");
    }

    public void requireNoCourseHistory(Long courseId) {
        if (scheduleMapper.selectCount(new LambdaQueryWrapper<FitCourseSchedule>().eq(FitCourseSchedule::getCourseId, courseId)) > 0)
            throw new ServiceException("课程已有排课，请使用下架功能保留历史");
    }

    public void confirmRecord(Long checkinId, Long operatorId, int times, BigDecimal amount, String remark) {
        if (times < 0 || amount == null || amount.signum() < 0) throw new ServiceException("核销课次和金额不得为负数");
        if (mapper.transitionCheckin(checkinId, tenant(), operatorId, "success", times, amount, remark) != 1)
            throw new ServiceException("签到已处理，请刷新后重试");
    }

    public void markException(Long checkinId, Long operatorId, String remark) {
        if (remark == null || remark.isBlank() || remark.length() > 500) throw new ServiceException("请填写500字以内的异常原因");
        if (mapper.transitionCheckin(checkinId, tenant(), operatorId, "reversed", 0, BigDecimal.ZERO, remark) != 1)
            throw new ServiceException("签到已处理，请刷新后重试");
    }

    @Transactional(rollbackFor = Exception.class)
    public Long proxyCheckin(FitnessEngagementBo.ProxyCheckin bo) {
        Long coachId = FitnessSecurityUtils.getCurrentCoachId();
        FitCourseReservation r = lockReservation(bo.getReservationId());
        FitCourseSchedule s = scheduleMapper.selectById(r.getScheduleId());
        if (s == null || !Objects.equals(s.getCoachId(), coachId)) throw new ServiceException("只能代签自己授课场次的会员");
        requireCoach(coachId, s.getStoreId(), false);
        FitCheckinRecord existing = checkinMapper.selectOne(new LambdaQueryWrapper<FitCheckinRecord>().eq(FitCheckinRecord::getReservationId, r.getReservationId()));
        if (existing != null) {
            if ("reversed".equals(existing.getCheckinStatus())) throw new ServiceException("此签到已标记异常，请联系管理员处理");
            return existing.getCheckinId();
        }
        if (!"reserved".equals(r.getReservationStatus())) throw new ServiceException("当前预约不可签到");
        requireCheckinWindow(r.getScheduleId());
        FitCheckinRecord record = new FitCheckinRecord();
        record.setCheckinId(IdGeneratorUtil.nextLongId());
        record.setCheckinNo(FitnessBizCodeUtils.defaultIfBlank(null, "CHK"));
        record.setTenantId(tenant()); record.setStoreId(r.getStoreId()); record.setScheduleId(r.getScheduleId());
        record.setMemberId(r.getMemberId()); record.setReservationId(r.getReservationId()); record.setCardId(r.getCardId());
        record.setCheckinType("coach_proxy"); record.setCheckinTime(new Date()); record.setCheckinStatus("success");
        record.setConsumeTimes(0); record.setConsumeAmount(BigDecimal.ZERO); record.setRemark(bo.getRemark());
        if (checkinMapper.insert(record) != 1) throw new ServiceException("代签到失败");
        audit(r.getMemberId(), r.getStoreId(), r.getScheduleId(), r.getReservationId(), record.getCheckinId(), "coach_proxy_checkin", bo.getSource(), bo.getRemark());
        return record.getCheckinId();
    }

    public void audit(Long memberId, Long storeId, Long scheduleId, Long reservationId, Long checkinId, String action, String source, String remark) {
        Object user = LoginHelper.getLoginUser();
        String type = user instanceof FitnessLoginUser.MemberLoginUser ? "member" : user instanceof FitnessLoginUser.CoachLoginUser ? "coach" : "admin";
        Long actorId = user instanceof FitnessLoginUser.MemberLoginUser member ? member.getMemberId() : LoginHelper.getUserId();
        source = "admin".equals(type) ? "web" : FitnessSecurityUtils.getCurrentSource();
        if (mapper.insertBehavior(IdGeneratorUtil.nextLongId(), tenant(), actorId, type, memberId, storeId, scheduleId, reservationId, checkinId, action, source, remark) != 1)
            throw new ServiceException("行为记录写入失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordInterest(Long courseId, FitnessEngagementBo.Interest bo) {
        Long memberId = FitnessSecurityUtils.getCurrentMemberId();
        // Serialize all tabs/devices for a member so simultaneous heartbeats cannot inflate wall time.
        FitMember member = memberMapper.selectOne(new LambdaQueryWrapper<FitMember>().eq(FitMember::getMemberId, memberId)
            .eq(FitMember::getTenantId, tenant()).last("FOR UPDATE"));
        if (member == null) throw new ServiceException("会员不存在");
        if (mapper.countInterestEvent(tenant(), memberId, bo.getEventId()) > 0) return;
        FitCourse c = courseMapper.selectById(courseId);
        if (c == null || !Objects.equals(c.getTenantId(), tenant()) || !"0".equals(c.getStatus())) throw new ServiceException("课程不存在或已下架");
        FitnessBookingPolicy.validateWindow(new Date(), c.getPublishedAt(), c.getListingUntil(), "课程上架");
        Date last = mapper.selectLastInterestTime(tenant(), memberId);
        int elapsed = last == null ? 60 : (int) Math.max(0, Math.min(60, (System.currentTimeMillis() - last.getTime()) / 1000));
        int seconds = Math.min(bo.getActiveSeconds(), elapsed);
        Long coach = mapper.selectInterestCoach(tenant(), memberId, c.getDefaultCoachId(), c.getStoreId());
        mapper.insertInterest(IdGeneratorUtil.nextLongId(), tenant(), memberId, courseId, coach, bo.getSessionId(), bo.getEventId(), seconds, FitnessSecurityUtils.getCurrentSource());
    }

    public TableDataInfo<FitnessEngagementVo.Behavior> behaviors(Long memberId, String action, PageQuery page) {
        return TableDataInfo.build(mapper.selectBehaviors(page.build(), tenant(), memberId, action));
    }
    public TableDataInfo<FitnessEngagementVo.Interest> interests(Long memberId, PageQuery page) {
        var coach = FitnessSecurityUtils.getCoachLoginUser();
        requireCoach(coach.getCoachId(), coach.getStoreId(), false);
        return TableDataInfo.build(mapper.selectInterests(page.build(), tenant(), coach.getCoachId(), memberId));
    }
}
