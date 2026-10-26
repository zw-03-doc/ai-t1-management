package org.dromara.fitness.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.constant.FitnessMobileConstants;
import org.dromara.fitness.domain.FitCoach;
import org.dromara.fitness.domain.FitCoachWechatBind;
import org.dromara.fitness.domain.FitCourseReservation;
import org.dromara.fitness.domain.dto.FitnessWechatMiniDto;
import org.dromara.fitness.domain.bo.FitnessCoachAppBo;
import org.dromara.fitness.domain.vo.FitnessCoachAppVo;
import org.dromara.fitness.domain.FitMemberCard;
import org.dromara.fitness.mapper.FitnessCoachAppMapper;
import org.dromara.fitness.mapper.FitnessCoachMapper;
import org.dromara.fitness.mapper.FitnessCoachWechatBindMapper;
import org.dromara.fitness.mapper.FitnessMemberCardMapper;
import org.dromara.fitness.mapper.FitnessReservationMapper;
import org.dromara.fitness.service.IFitnessCoachAppService;
import org.dromara.fitness.service.support.FitnessWechatMiniAuthService;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.dromara.system.domain.SysUser;
import org.dromara.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.dromara.fitness.service.support.FitnessOperationsService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 教练 App 服务实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessCoachAppServiceImpl implements IFitnessCoachAppService {

    private final FitnessOperationsService operations;
    private final FitnessCoachAppMapper baseMapper;
    private final FitnessCoachMapper coachMapper;
    private final FitnessCoachWechatBindMapper coachWechatBindMapper;
    private final SysUserMapper sysUserMapper;
    private final FitnessWechatMiniAuthService wechatMiniAuthService;
    private final FitnessReservationMapper reservationMapper;
    private final FitnessMemberCardMapper memberCardMapper;

    @Override
    public FitnessCoachAppVo.LoginVo login(FitnessCoachAppBo.LoginBo bo) {
        String accountKey = "fitness:coach-login:" + cn.hutool.crypto.digest.DigestUtil.sha256Hex(bo.getUsername().trim().toLowerCase(java.util.Locale.ROOT));
        if (org.dromara.common.redis.utils.RedisUtils.rateLimiter(accountKey, org.redisson.api.RateType.OVERALL, 5, 60) < 0)
            throw new ServiceException("账号登录尝试过于频繁，请1分钟后重试");
        FitnessCoachAppVo.CoachProfileVo authInfo = baseMapper.selectCoachAuthInfo(bo.getUsername());
        validateCoachAuthInfo(authInfo, true);
        if (StringUtils.isBlank(authInfo.getPassword()) || !BCrypt.checkpw(bo.getPassword(), authInfo.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }
        return buildLoginVo(authInfo, null, true, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FitnessCoachAppVo.LoginVo wechatLogin(FitnessCoachAppBo.WechatLoginBo bo) {
        String openid = resolveOpenid(bo.getCode(), bo.getOpenid());
        FitCoachWechatBind bind = getWechatBindByOpenid(openid);
        if (bind == null) {
            return buildPendingActivationVo(openid);
        }
        ensureBindActive(bind);
        return buildActivatedLoginByBind(bind, openid, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FitnessCoachAppVo.LoginVo wechatActivate(FitnessCoachAppBo.WechatActivateBo bo) {
        String openid = resolveOpenid(bo.getCode(), bo.getOpenid());
        FitCoachWechatBind currentBind = getWechatBindByOpenid(openid);
        if (currentBind != null) {
            ensureBindActive(currentBind);
            return buildActivatedLoginByBind(currentBind, openid, "当前微信已完成激活");
        }
        FitCoachWechatBind bind = getWechatBindByMobileAndCode(bo.getMobile(), bo.getActivationCode());
        if (bind == null) {
            throw new ServiceException("手机号或激活码错误");
        }
        ensureBindActive(bind);
        if (StringUtils.isNotBlank(bind.getWechatOpenid()) && !Objects.equals(bind.getWechatOpenid(), openid)) {
            throw new ServiceException("此身份已激活，请联系店长");
        }
        bind.setWechatOpenid(openid);
        if (bind.getActivateTime() == null) {
            bind.setActivateTime(new Date());
        }
        coachWechatBindMapper.updateById(bind);
        return buildActivatedLoginByBind(bind, openid, "激活成功");
    }

    @Override
    public void logout() {
        FitnessSecurityUtils.logout();
    }

    @Override
    public FitnessCoachAppVo.HomeOverviewVo queryHomeOverview() {
        Long coachId = FitnessSecurityUtils.getCurrentCoachId();
        FitnessCoachAppVo.HomeOverviewVo overviewVo = new FitnessCoachAppVo.HomeOverviewVo();
        overviewVo.setStats(fillHomeStats(baseMapper.selectCoachHomeStats(coachId)));
        FitnessCoachAppBo.ScheduleQueryBo scheduleBo = new FitnessCoachAppBo.ScheduleQueryBo();
        scheduleBo.setScope("today");
        List<FitnessCoachAppVo.ScheduleVo> todaySchedules = defaultList(baseMapper.selectCoachScheduleList(coachId, scheduleBo));
        overviewVo.setTodayScheduleList(limitList(todaySchedules, 4));
        FitnessCoachAppBo.CheckinQueryBo checkinBo = new FitnessCoachAppBo.CheckinQueryBo();
        checkinBo.setStatus("pending");
        List<FitnessCoachAppVo.CheckinVo> pendingList = defaultList(baseMapper.selectCoachCheckinList(coachId, checkinBo));
        pendingList.forEach(this::fillCheckinStatus);
        overviewVo.setPendingList(limitList(pendingList, 4));
        overviewVo.setMessageList(buildMessageList(todaySchedules, pendingList));
        return overviewVo;
    }

    @Override
    public TableDataInfo<FitnessCoachAppVo.ScheduleVo> queryMySchedule(FitnessCoachAppBo.ScheduleQueryBo bo, PageQuery pageQuery) {
        List<FitnessCoachAppVo.ScheduleVo> list = defaultList(baseMapper.selectCoachScheduleList(FitnessSecurityUtils.getCurrentCoachId(), bo));
        return TableDataInfo.build(list, pageQuery.build());
    }

    @Override
    public FitnessCoachAppVo.ScheduleVo queryScheduleDetail(Long scheduleId) {
        FitnessCoachAppVo.ScheduleVo vo = baseMapper.selectCoachScheduleDetail(FitnessSecurityUtils.getCurrentCoachId(), scheduleId);
        if (vo == null) {
            throw new ServiceException("排课不存在");
        }
        return vo;
    }

    @Override
    public TableDataInfo<FitnessCoachAppVo.CheckinVo> queryCheckinList(FitnessCoachAppBo.CheckinQueryBo bo, PageQuery pageQuery) {
        List<FitnessCoachAppVo.CheckinVo> list = defaultList(baseMapper.selectCoachCheckinList(FitnessSecurityUtils.getCurrentCoachId(), bo));
        list.forEach(this::fillCheckinStatus);
        return TableDataInfo.build(list, pageQuery.build());
    }

    @Override
    public FitnessCoachAppVo.CheckinVo queryCheckinDetail(Long reservationId) {
        FitnessCoachAppVo.CheckinVo vo = baseMapper.selectCoachCheckinDetail(FitnessSecurityUtils.getCurrentCoachId(), reservationId);
        if (vo == null) {
            throw new ServiceException("核销记录不存在");
        }
        fillCheckinStatus(vo);
        return vo;
    }

    @Override
    @Lock4j(keys = {"'checkin:confirm:' + #bo.reservationId"}, acquireTimeout = 3000)
    @Transactional(rollbackFor = Exception.class)
    public FitnessCoachAppVo.CheckinVo confirmCheckin(FitnessCoachAppBo.CheckinConfirmBo bo) {
        operations.lockReservation(bo.getReservationId());
        Long coachId = FitnessSecurityUtils.getCurrentCoachId();
        Long operatorUserId = FitnessSecurityUtils.getCurrentCoachUserId();
        FitnessCoachAppVo.CheckinActionVo actionVo = baseMapper.selectCoachCheckinAction(coachId, bo.getReservationId());
        if (actionVo == null) {
            throw new ServiceException("待核销记录不存在");
        }
        if (Objects.equals("reversed", actionVo.getCheckinStatus())) {
            throw new ServiceException("该签到已标记异常");
        }
        if (actionVo.getOperatorUserId() != null) {
            throw new ServiceException("该签到已确认核销");
        }
        int consumeTimes = bo.getConsumeTimes() == null
            ? (actionVo.getCardId() == null || Objects.equals("time_card", actionVo.getPlanType()) ? 0 : 1)
            : bo.getConsumeTimes();

        BigDecimal consumeAmount = bo.getConsumeAmount() == null ? BigDecimal.ZERO : bo.getConsumeAmount();
        validateCardConsume(actionVo, consumeTimes, consumeAmount);
        operations.confirmRecord(actionVo.getCheckinId(), operatorUserId, consumeTimes, consumeAmount, bo.getRemark());
        baseMapper.updateScheduleSignedCount(actionVo.getScheduleId(), 1);
        if (actionVo.getCardId() != null && (consumeTimes > 0 || consumeAmount.compareTo(BigDecimal.ZERO) > 0)) {
            consumeMemberCard(actionVo.getCardId(), consumeTimes, consumeAmount);
        }
        if (actionVo.getReservationId() != null) {
            FitCourseReservation reservation = new FitCourseReservation();
            reservation.setReservationId(actionVo.getReservationId());
            reservation.setReservationStatus("signed");
            reservationMapper.updateById(reservation);
        }
        operations.audit(actionVo.getMemberId(), actionVo.getStoreId(), actionVo.getScheduleId(), bo.getReservationId(), actionVo.getCheckinId(), "coach_confirm_checkin", StringUtils.defaultIfBlank(bo.getSource(), "mini"), bo.getRemark());
        return queryCheckinDetail(bo.getReservationId());
    }

    @Override
    @Lock4j(keys = {"'checkin:exception:' + #bo.reservationId"}, acquireTimeout = 3000)
    @Transactional(rollbackFor = Exception.class)
    public FitnessCoachAppVo.CheckinVo markCheckinException(FitnessCoachAppBo.CheckinExceptionBo bo) {
        operations.lockReservation(bo.getReservationId());
        Long coachId = FitnessSecurityUtils.getCurrentCoachId();
        Long operatorUserId = FitnessSecurityUtils.getCurrentCoachUserId();
        FitnessCoachAppVo.CheckinActionVo actionVo = baseMapper.selectCoachCheckinAction(coachId, bo.getReservationId());
        if (actionVo == null) {
            throw new ServiceException("待核销记录不存在");
        }
        if (actionVo.getOperatorUserId() != null && Objects.equals("success", actionVo.getCheckinStatus())) {
            throw new ServiceException("该签到已确认，不能再标记异常");
        }
        operations.markException(actionVo.getCheckinId(), operatorUserId, bo.getRemark());
        operations.audit(actionVo.getMemberId(), actionVo.getStoreId(), actionVo.getScheduleId(), bo.getReservationId(), actionVo.getCheckinId(), "coach_checkin_exception", StringUtils.defaultIfBlank(bo.getSource(), "mini"), bo.getRemark());
        return queryCheckinDetail(bo.getReservationId());
    }

    @Override
    public TableDataInfo<FitnessCoachAppVo.StudentVo> queryMyStudents(String keyword, PageQuery pageQuery) {
        List<FitnessCoachAppVo.StudentVo> list = defaultList(baseMapper.selectCoachStudentList(FitnessSecurityUtils.getCurrentCoachId(), keyword));
        return TableDataInfo.build(list, pageQuery.build());
    }

    @Override
    public FitnessCoachAppVo.StudentVo queryStudentDetail(Long memberId) {
        FitnessCoachAppVo.StudentVo vo = baseMapper.selectCoachStudentDetail(FitnessSecurityUtils.getCurrentCoachId(), memberId);
        if (vo == null) {
            throw new ServiceException("学员不存在");
        }
        return vo;
    }

    @Override
    public FitnessCoachAppVo.CoachProfileVo queryProfile() {
        FitnessCoachAppVo.CoachProfileVo profileVo = baseMapper.selectCoachProfileByUserId(FitnessSecurityUtils.getCurrentCoachUserId());
        if (profileVo == null) {
            throw new ServiceException("教练资料不存在");
        }
        profileVo.setPassword(null);
        return profileVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(FitnessCoachAppBo.ProfileUpdateBo bo) {
        Long coachId = FitnessSecurityUtils.getCurrentCoachId();
        Long userId = FitnessSecurityUtils.getCurrentCoachUserId();
        FitCoach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException("教练资料不存在");
        }
        if (StringUtils.isNotBlank(bo.getMobile()) && !Objects.equals(bo.getMobile(), coach.getMobile())
            && coachMapper.exists(new LambdaQueryWrapper<FitCoach>()
            .eq(FitCoach::getMobile, bo.getMobile())
            .ne(FitCoach::getCoachId, coachId))) {
            throw new ServiceException("手机号已存在");
        }
        if (baseMapper.updateCoachProfile(coachId, bo) <= 0) {
            throw new ServiceException("更新教练资料失败");
        }
        SysUser update = new SysUser();
        update.setUserId(userId);
        if (StringUtils.isNotBlank(bo.getNickName())) {
            update.setNickName(bo.getNickName());
        }
        if (StringUtils.isNotBlank(bo.getMobile())) {
            update.setPhonenumber(bo.getMobile());
        }
        sysUserMapper.updateById(update);
        if (StringUtils.isNotBlank(bo.getMobile())) {
            syncCoachWechatBindMobile(coachId, bo.getMobile());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(FitnessCoachAppBo.PasswordUpdateBo bo) {
        Long userId = FitnessSecurityUtils.getCurrentCoachUserId();
        SysUser sysUser = sysUserMapper.selectById(userId);
        if (sysUser == null) {
            throw new ServiceException("当前教练账号不存在");
        }
        if (!BCrypt.checkpw(bo.getOldPassword(), sysUser.getPassword())) {
            throw new ServiceException("旧密码错误");
        }
        if (BCrypt.checkpw(bo.getNewPassword(), sysUser.getPassword())) {
            throw new ServiceException("新密码不能与旧密码相同");
        }
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setPassword(BCrypt.hashpw(bo.getNewPassword()));
        sysUserMapper.updateById(update);
    }

    @Override
    public TableDataInfo<FitnessCoachAppVo.MessageVo> queryMessageList(PageQuery pageQuery) {
        List<FitnessCoachAppVo.ScheduleVo> todaySchedules = defaultList(baseMapper.selectCoachScheduleList(FitnessSecurityUtils.getCurrentCoachId(), buildTodayScheduleBo()));
        List<FitnessCoachAppVo.CheckinVo> pendingList = defaultList(baseMapper.selectCoachCheckinList(FitnessSecurityUtils.getCurrentCoachId(), buildPendingCheckinBo()));
        pendingList.forEach(this::fillCheckinStatus);
        List<FitnessCoachAppVo.MessageVo> messageList = buildMessageList(todaySchedules, pendingList);
        return TableDataInfo.build(messageList, pageQuery.build());
    }

    private FitnessCoachAppVo.LoginVo buildPendingActivationVo(String openid) {
        FitnessCoachAppVo.LoginVo loginVo = new FitnessCoachAppVo.LoginVo();
        loginVo.setClientId(FitnessMobileConstants.COACH_CLIENT_ID);
        loginVo.setActivated(false);
        loginVo.setNeedActivation(true);
        loginVo.setOpenid(openid);
        loginVo.setMessage("请先输入手机号和激活码完成激活");
        return loginVo;
    }

    private FitnessCoachAppVo.LoginVo buildActivatedLoginByBind(FitCoachWechatBind bind, String openid, String message) {
        FitnessCoachAppVo.CoachProfileVo authInfo = baseMapper.selectCoachAuthInfoByCoachId(bind.getCoachId());
        validateCoachAuthInfo(authInfo, false);
        return buildLoginVo(authInfo, openid, true, message);
    }

    private FitnessCoachAppVo.LoginVo buildLoginVo(FitnessCoachAppVo.CoachProfileVo authInfo,
                                                   String openid,
                                                   boolean activated,
                                                   String message) {
        FitnessSecurityUtils.loginCoach(authInfo, openid == null ? "web" : "mini");
        authInfo.setPassword(null);
        FitnessCoachAppVo.LoginVo loginVo = new FitnessCoachAppVo.LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(FitnessMobileConstants.COACH_CLIENT_ID);
        loginVo.setActivated(activated);
        loginVo.setNeedActivation(!activated);
        loginVo.setOpenid(openid);
        loginVo.setMessage(message);
        loginVo.setProfile(authInfo);
        return loginVo;
    }

    private String resolveOpenid(String code, String openid) {
        if (StringUtils.isBlank(code)) throw new ServiceException("请通过微信登录凭证登录");
        FitnessWechatMiniDto.SessionInfo sessionInfo = wechatMiniAuthService.code2Session(code);
        openid = sessionInfo.getOpenid();
        if (StringUtils.isBlank(openid)) throw new ServiceException("微信登录未返回有效身份");
        return openid;
    }

    private FitCoachWechatBind getWechatBindByOpenid(String openid) {
        return coachWechatBindMapper.selectOne(new LambdaQueryWrapper<FitCoachWechatBind>()
            .eq(FitCoachWechatBind::getWechatOpenid, openid)
            .last("LIMIT 1"));
    }

    private FitCoachWechatBind getWechatBindByMobileAndCode(String mobile, String activationCode) {
        return coachWechatBindMapper.selectOne(new LambdaQueryWrapper<FitCoachWechatBind>()
            .eq(FitCoachWechatBind::getCoachMobile, mobile)
            .eq(FitCoachWechatBind::getActivationCode, activationCode)
            .last("LIMIT 1"));
    }

    private void ensureBindActive(FitCoachWechatBind bind) {
        if (bind == null) {
            throw new ServiceException("教练绑定关系不存在");
        }
        if (!Objects.equals("0", bind.getBindStatus())) {
            throw new ServiceException("教练账号已停用");
        }
    }

    private void validateCoachAuthInfo(FitnessCoachAppVo.CoachProfileVo authInfo, boolean usernameLogin) {
        if (authInfo == null) {
            throw new ServiceException(usernameLogin ? "教练账号不存在" : "教练身份不存在或已被删除");
        }
        if (!Objects.equals("0", authInfo.getHireStatus()) || !Objects.equals("0", authInfo.getUserStatus())) {
            throw new ServiceException("教练账号已停用");
        }
    }

    private void syncCoachWechatBindMobile(Long coachId, String mobile) {
        FitCoachWechatBind bind = coachWechatBindMapper.selectOne(new LambdaQueryWrapper<FitCoachWechatBind>()
            .eq(FitCoachWechatBind::getCoachId, coachId)
            .last("LIMIT 1"));
        if (bind == null) {
            return;
        }
        bind.setCoachMobile(mobile);
        coachWechatBindMapper.updateById(bind);
    }

    private FitnessCoachAppBo.ScheduleQueryBo buildTodayScheduleBo() {
        FitnessCoachAppBo.ScheduleQueryBo bo = new FitnessCoachAppBo.ScheduleQueryBo();
        bo.setScope("today");
        return bo;
    }

    private FitnessCoachAppBo.CheckinQueryBo buildPendingCheckinBo() {
        FitnessCoachAppBo.CheckinQueryBo bo = new FitnessCoachAppBo.CheckinQueryBo();
        bo.setStatus("pending");
        return bo;
    }

    private void validateCardConsume(FitnessCoachAppVo.CheckinActionVo actionVo, int consumeTimes, BigDecimal consumeAmount) {
        if (consumeTimes < 0 || consumeAmount.signum() < 0) throw new ServiceException("核销课次和金额不得为负数");
        if (actionVo.getCardId() == null) {
            if (consumeTimes != 0 || consumeAmount.signum() != 0) throw new ServiceException("无卡预约不能扣除卡权益");
            return;
        }
        if (!Objects.equals("unused", actionVo.getCardStatus()) && !Objects.equals("active", actionVo.getCardStatus())) {
            throw new ServiceException("会员卡状态不可核销");
        }
        if (Objects.equals("time_card", actionVo.getPlanType())) {
            if (consumeTimes > 0 || consumeAmount.compareTo(BigDecimal.ZERO) > 0) {
                throw new ServiceException("时间卡无需核销扣减");
            }
            return;
        }

        if (consumeTimes > 0 && actionVo.getRemainTimes() != null && consumeTimes > actionVo.getRemainTimes()) {
            throw new ServiceException("剩余课次不足");
        }
        if (consumeAmount.compareTo(BigDecimal.ZERO) > 0 && actionVo.getBalanceAmount() != null
            && consumeAmount.compareTo(actionVo.getBalanceAmount()) > 0) {
            throw new ServiceException("余额不足，无法完成核销");
        }
    }

    private void consumeMemberCard(Long cardId, int consumeTimes, BigDecimal consumeAmount) {
        FitMemberCard card = memberCardMapper.selectOne(new LambdaQueryWrapper<FitMemberCard>()
            .eq(FitMemberCard::getCardId, cardId).last("FOR UPDATE"));
        if (card == null) {
            throw new ServiceException("会员卡不存在");
        }
        if (!StringUtils.equalsAny(card.getCardStatus(), "unused", "active")
            || card.getExpireTime() != null && !new Date().before(card.getExpireTime())) throw new ServiceException("会员卡已失效");
        if (consumeTimes > defaultInt(card.getRemainTimes())) throw new ServiceException("剩余课次不足");
        int usedTimes = defaultInt(card.getUsedTimes()) + Math.max(consumeTimes, 0);
        int remainTimes = Math.max(defaultInt(card.getRemainTimes()) - Math.max(consumeTimes, 0), 0);
        BigDecimal balanceAmount = defaultDecimal(card.getBalanceAmount()).subtract(consumeAmount == null ? BigDecimal.ZERO : consumeAmount);
        if (balanceAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("会员卡余额不足");
        }
        FitMemberCard update = new FitMemberCard();
        update.setCardId(card.getCardId());
        update.setVersion(card.getVersion());
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

    private void fillCheckinStatus(FitnessCoachAppVo.CheckinVo vo) {
        if (vo.getCheckinId() == null) { vo.setDerivedCheckinStatus(FitnessMobileConstants.CHECKIN_NOT_STARTED); return; }
        if (Objects.equals("reversed", vo.getCheckinStatus())) {
            vo.setDerivedCheckinStatus(FitnessMobileConstants.CHECKIN_EXCEPTION);
            return;
        }
        if (vo.getOperatorUserId() != null) {
            vo.setDerivedCheckinStatus(FitnessMobileConstants.CHECKIN_CONFIRMED);
            return;
        }
        vo.setDerivedCheckinStatus(FitnessMobileConstants.CHECKIN_PENDING_CONFIRM);
    }

    private FitnessCoachAppVo.HomeStatsVo fillHomeStats(FitnessCoachAppVo.HomeStatsVo statsVo) {
        FitnessCoachAppVo.HomeStatsVo result = statsVo == null ? new FitnessCoachAppVo.HomeStatsVo() : statsVo;
        result.setTodayScheduleCount(defaultLong(result.getTodayScheduleCount()));
        result.setPendingCheckinCount(defaultLong(result.getPendingCheckinCount()));
        result.setConfirmedCount(defaultLong(result.getConfirmedCount()));
        result.setTotalStudentCount(defaultLong(result.getTotalStudentCount()));
        result.setWeekSessionCount(defaultLong(result.getWeekSessionCount()));
        return result;
    }

    private List<FitnessCoachAppVo.MessageVo> buildMessageList(List<FitnessCoachAppVo.ScheduleVo> scheduleList,
                                                               List<FitnessCoachAppVo.CheckinVo> pendingList) {
        List<FitnessCoachAppVo.MessageVo> result = new ArrayList<>();
        pendingList.stream().limit(6).forEach(item -> {
            FitnessCoachAppVo.MessageVo vo = new FitnessCoachAppVo.MessageVo();
            vo.setMessageType("checkin");
            vo.setTitle("待核销提醒");
            vo.setContent(item.getMemberName() + " 已签到《" + item.getCourseName() + "》，请尽快确认核销");
            vo.setEventTime(item.getCheckinTime());
            vo.setRoute("/pages/coach/checkin/index");
            result.add(vo);
        });
        scheduleList.stream().limit(6).forEach(item -> {
            FitnessCoachAppVo.MessageVo vo = new FitnessCoachAppVo.MessageVo();
            vo.setMessageType("schedule");
            vo.setTitle("今日排课提醒");
            vo.setContent(item.getCourseName() + " 将于今日开课，请关注到课与签到情况");
            vo.setEventTime(item.getStartTime());
            vo.setRoute("/pages/coach/schedule/index");
            result.add(vo);
        });
        return result.stream()
            .sorted((a, b) -> {
                Date left = a.getEventTime() == null ? new Date(0L) : a.getEventTime();
                Date right = b.getEventTime() == null ? new Date(0L) : b.getEventTime();
                return right.compareTo(left);
            })
            .limit(10)
            .collect(Collectors.toList());
    }

    private <T> List<T> defaultList(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

    private <T> List<T> limitList(List<T> list, int size) {
        if (list.size() <= size) {
            return list;
        }
        return new ArrayList<>(list.subList(0, size));
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}

