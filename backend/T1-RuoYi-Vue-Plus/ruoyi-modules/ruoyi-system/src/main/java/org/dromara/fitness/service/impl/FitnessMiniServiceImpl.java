package org.dromara.fitness.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.fitness.constant.FitnessMobileConstants;
import org.dromara.fitness.domain.bo.FitnessMiniBo;
import org.dromara.fitness.domain.dto.FitnessWechatMiniDto;
import org.dromara.fitness.domain.vo.FitnessMiniVo;
import org.dromara.fitness.mapper.FitnessMiniMapper;
import org.dromara.fitness.service.IFitnessMiniService;
import org.dromara.fitness.service.support.FitnessWechatMiniAuthService;
import org.dromara.fitness.utils.FitnessBizCodeUtils;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 会员小程序服务实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessMiniServiceImpl implements IFitnessMiniService {

    private static final List<String> DEFAULT_MEMBER_NICKNAMES = List.of(
        "好事花生", "平安喜乐", "万事顺意", "前程似锦", "吉星高照", "福运连连",
        "顺风顺水", "喜乐长安", "所愿成真", "春和景明", "自在欢喜", "心想事成"
    );
    private static final Set<String> BAD_LUCKY_SUFFIXES = Set.of("4444", "4848", "7414", "1474", "8147", "4242");

    private final FitnessMiniMapper baseMapper;
    private final FitnessWechatMiniAuthService wechatMiniAuthService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FitnessMiniVo.LoginVo wechatLogin(FitnessMiniBo.WechatLoginBo bo) {
        if (StringUtils.isBlank(bo.getOpenid())) {
            FitnessWechatMiniDto.SessionInfo sessionInfo = wechatMiniAuthService.code2Session(bo.getCode());
            bo.setOpenid(sessionInfo.getOpenid());
            if (StringUtils.isBlank(bo.getUnionId())) {
                bo.setUnionId(sessionInfo.getUnionId());
            }
        }
        if (StringUtils.isBlank(bo.getOpenid())) {
            throw new ServiceException("微信登录失败，未获取到 openid");
        }
        FitnessMiniVo.MemberInfoVo memberInfoVo = baseMapper.selectMemberByOpenid(bo.getOpenid());
        if (memberInfoVo == null) {
            if (StringUtils.isBlank(bo.getNickname())) {
                bo.setNickname(buildDefaultNickname());
            }
            Long memberId = IdGeneratorUtil.nextLongId();
            String memberNo = "MEM" + IdGeneratorUtil.nextId();
            String tenantId = TenantConstants.DEFAULT_TENANT_ID;
            baseMapper.insertMember(memberId, memberNo, tenantId, bo);
            memberInfoVo = baseMapper.selectMemberById(memberId);
        } else {
            if (StringUtils.isBlank(bo.getNickname()) && shouldRefreshDefaultNickname(memberInfoVo.getNickname())) {
                bo.setNickname(buildDefaultNickname());
            }
            baseMapper.updateMemberLoginProfile(memberInfoVo.getMemberId(), bo);
            memberInfoVo = baseMapper.selectMemberById(memberInfoVo.getMemberId());
        }
        if (memberInfoVo == null) {
            throw new ServiceException("会员登录失败，请稍后重试");
        }
        FitnessSecurityUtils.loginMember(memberInfoVo, bo.getOpenid());
        FitnessMiniVo.LoginVo loginVo = new FitnessMiniVo.LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(FitnessMobileConstants.MINI_CLIENT_ID);
        loginVo.setOpenid(bo.getOpenid());
        loginVo.setMemberInfo(memberInfoVo);
        return loginVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindPhone(FitnessMiniBo.BindPhoneBo bo) {
        FitnessWechatMiniDto.PhoneInfo phoneInfo = wechatMiniAuthService.getPhoneInfo(bo.getPhoneCode());
        String mobile = StringUtils.defaultIfBlank(phoneInfo.getPhoneNumber(), phoneInfo.getPurePhoneNumber());
        if (StringUtils.isBlank(mobile)) {
            throw new ServiceException("获取微信手机号失败");
        }
        bo.setMobile(mobile);
        Long memberId = FitnessSecurityUtils.getCurrentMemberId();
        String tenantId = FitnessSecurityUtils.getCurrentTenantId();
        Long count = baseMapper.countMemberByMobile(tenantId, mobile, memberId);
        if (count != null && count > 0) {
            throw new ServiceException("该手机号已被其他会员绑定");
        }
        if (baseMapper.updateMemberPhone(memberId, bo) <= 0) {
            throw new ServiceException("绑定手机号失败");
        }
    }

    @Override
    public FitnessMiniVo.HomeIndexVo queryHomeIndex() {
        Long memberId = FitnessSecurityUtils.getCurrentMemberId();
        FitnessMiniVo.MemberInfoVo memberInfoVo = requireMember(memberId);
        FitnessMiniVo.HomeIndexVo homeIndexVo = new FitnessMiniVo.HomeIndexVo();
        homeIndexVo.setMineSummary(fillMineSummary(baseMapper.selectMineSummary(memberId), memberInfoVo));
        List<FitnessMiniVo.CourseVo> recommendCourseList = defaultList(baseMapper.selectRecommendCourseList(memberId));
        homeIndexVo.setRecommendCourseList(limitList(recommendCourseList, 4));
        List<FitnessMiniVo.ReservationVo> reservationList = defaultList(baseMapper.selectMyReservationList(memberId));
        reservationList.forEach(this::fillReservationCheckinStatus);
        homeIndexVo.setUpcomingReservationList(limitList(reservationList.stream()
            .filter(item -> Objects.equals("reserved", item.getReservationStatus()))
            .collect(Collectors.toList()), 3));
        homeIndexVo.setCardList(limitList(defaultList(baseMapper.selectMyMemberCardList(memberId)), 3));
        homeIndexVo.setPromotionList(limitList(defaultList(baseMapper.selectCardPlanList(memberInfoVo.getHomeStoreId())), 3));
        homeIndexVo.setBannerList(buildBannerList(memberId, recommendCourseList));
        return homeIndexVo;
    }

    @Override
    public List<FitnessMiniVo.StoreVo> queryStoreList(String keyword) {
        List<FitnessMiniVo.StoreVo> list = defaultList(baseMapper.selectStoreList(keyword, FitnessSecurityUtils.getCurrentMemberId()));
        return list;
    }

    @Override
    public FitnessMiniVo.StoreVo queryStoreById(Long storeId) {
        FitnessMiniVo.StoreVo vo = baseMapper.selectStoreById(storeId, FitnessSecurityUtils.getCurrentMemberId());
        if (vo == null) {
            throw new ServiceException("门店不存在");
        }
        return vo;
    }

    @Override
    public List<FitnessMiniVo.CourseVo> queryCourseList(FitnessMiniBo.CourseQueryBo bo) {
        return defaultList(baseMapper.selectCourseList(bo));
    }

    @Override
    public FitnessMiniVo.CourseVo queryCourseById(Long courseId) {
        FitnessMiniVo.CourseVo vo = baseMapper.selectCourseById(courseId);
        if (vo == null) {
            throw new ServiceException("课程不存在");
        }
        return vo;
    }

    @Override
    public TableDataInfo<FitnessMiniVo.ScheduleVo> queryScheduleList(FitnessMiniBo.ScheduleQueryBo bo, PageQuery pageQuery) {
        List<FitnessMiniVo.ScheduleVo> list = defaultList(baseMapper.selectScheduleList(FitnessSecurityUtils.getCurrentMemberId(), bo));
        list.forEach(this::fillScheduleCheckinStatus);
        return TableDataInfo.build(list, pageQuery.build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FitnessMiniVo.ReservationVo createReservation(FitnessMiniBo.ReservationCreateBo bo) {
        Long memberId = FitnessSecurityUtils.getCurrentMemberId();
        FitnessMiniVo.ReservationActionVo actionVo = baseMapper.selectScheduleReservationAction(memberId, bo.getScheduleId());
        if (actionVo == null) {
            throw new ServiceException("排课不存在或已下架");
        }
        Date now = new Date();
        if (actionVo.getReservationDeadline() != null && now.after(actionVo.getReservationDeadline())) {
            throw new ServiceException("当前排课已过预约截止时间");
        }
        if (actionVo.getStartTime() != null && now.after(actionVo.getStartTime())) {
            throw new ServiceException("课程已开始，无法预约");
        }
        if (actionVo.getCapacity() != null && actionVo.getReservedCount() != null
            && actionVo.getReservedCount() >= actionVo.getCapacity()) {
            throw new ServiceException("当前排课预约已满");
        }
        Long activeCount = baseMapper.countActiveReservation(memberId, bo.getScheduleId());
        if (activeCount != null && activeCount > 0) {
            throw new ServiceException("您已预约该课程，无需重复预约");
        }
        Long cardId = bo.getCardId();
        String reservationType = "store_experience";
        String consumeMode = "free";
        if (cardId != null) {
            FitnessMiniVo.MemberCardVo cardVo = requireUsableCard(memberId, cardId);
            validateReservationCardRule(cardVo, actionVo);
            reservationType = "card";
            consumeMode = resolveConsumeMode(cardVo);
        } else if (Objects.equals("private", actionVo.getCourseType()) || Objects.equals("small", actionVo.getCourseType())) {
            throw new ServiceException("私教/小班课程预约需选择对应卡项");
        }

        Long reservationId = IdGeneratorUtil.nextLongId();
        String reservationNo = FitnessBizCodeUtils.defaultIfBlank(null, "RSV");
        int rows = baseMapper.insertReservation(reservationId,
            reservationNo,
            actionVo.getTenantId() == null ? FitnessSecurityUtils.getCurrentTenantId() : actionVo.getTenantId(),
            memberId,
            actionVo.getScheduleId(),
            actionVo.getStoreId(),
            cardId,
            reservationType,
            consumeMode,
            bo.getRemark(),
            now);

        if (rows <= 0) {
            throw new ServiceException("预约课程失败");
        }
        baseMapper.updateScheduleReservedCount(actionVo.getScheduleId(), 1);
        FitnessMiniVo.ReservationVo result = baseMapper.selectReservationById(memberId, reservationId);
        if (result == null) {
            throw new ServiceException("预约结果查询失败");
        }
        fillReservationCheckinStatus(result);
        return result;
    }

    @Override
    public TableDataInfo<FitnessMiniVo.ReservationVo> queryMyReservationList(PageQuery pageQuery) {
        List<FitnessMiniVo.ReservationVo> list = defaultList(baseMapper.selectMyReservationList(FitnessSecurityUtils.getCurrentMemberId()));
        list.forEach(this::fillReservationCheckinStatus);
        return TableDataInfo.build(list, pageQuery.build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReservation(Long reservationId) {
        Long memberId = FitnessSecurityUtils.getCurrentMemberId();
        FitnessMiniVo.ReservationActionVo actionVo = baseMapper.selectReservationAction(memberId, reservationId);
        if (actionVo == null) {
            throw new ServiceException("预约记录不存在");
        }
        if (!Objects.equals("reserved", actionVo.getReservationStatus())) {
            throw new ServiceException("当前预约状态不可取消");
        }
        Date now = new Date();
        if (actionVo.getCancelDeadline() != null && now.after(actionVo.getCancelDeadline())) {
            throw new ServiceException("已超过取消预约截止时间");
        }
        if (actionVo.getStartTime() != null && now.after(actionVo.getStartTime())) {
            throw new ServiceException("课程已开始，无法取消预约");
        }
        if (baseMapper.updateReservationCancel(reservationId, "member_cancel", now) <= 0) {
            throw new ServiceException("取消预约失败");
        }
        baseMapper.updateScheduleReservedCount(actionVo.getScheduleId(), -1);
    }

    @Override
    public List<FitnessMiniVo.CardPlanVo> queryCardPlanList(Long storeId) {
        return defaultList(baseMapper.selectCardPlanList(storeId));
    }

    @Override
    public FitnessMiniVo.CardPlanVo queryCardPlanById(Long planId) {
        FitnessMiniVo.CardPlanVo vo = baseMapper.selectCardPlanById(planId);
        if (vo == null) {
            throw new ServiceException("卡项方案不存在");
        }
        return vo;
    }

    @Override
    public List<FitnessMiniVo.ActivityVo> queryActivityList(Long storeId) {
        return defaultList(baseMapper.selectActivityList(storeId));
    }

    @Override
    public FitnessMiniVo.ActivityVo queryActivityById(Long activityId) {
        FitnessMiniVo.ActivityVo vo = baseMapper.selectActivityById(activityId);
        if (vo == null) {
            throw new ServiceException("活动不存在或已下架");
        }
        vo.setPlanList(defaultList(baseMapper.selectActivityCardPlanList(activityId)));
        return vo;
    }

    @Override
    public List<FitnessMiniVo.ActivityCardPlanVo> queryActivityCardPlanList(Long activityId) {
        FitnessMiniVo.ActivityVo vo = baseMapper.selectActivityById(activityId);
        if (vo == null) {
            throw new ServiceException("活动不存在或已下架");
        }
        return defaultList(baseMapper.selectActivityCardPlanList(activityId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FitnessMiniVo.OrderVo createOrder(FitnessMiniBo.OrderCreateBo bo) {


        Long memberId = FitnessSecurityUtils.getCurrentMemberId();
        String tenantId = FitnessSecurityUtils.getCurrentTenantId();
        FitnessMiniVo.CardPlanVo planVo = queryCardPlanById(bo.getPlanId());
        Long orderId = IdGeneratorUtil.nextLongId();
        String orderNo = "ORD" + IdGeneratorUtil.nextId();
        boolean payNow = Boolean.TRUE.equals(bo.getPayNow());
        BigDecimal payableAmount = nullSafe(planVo.getSalePrice());
        BigDecimal paidAmount = payNow ? payableAmount : BigDecimal.ZERO;
        Date paidTime = payNow ? new Date() : null;
        String orderStatus = payNow ? "paid" : "pending";
        String payMethod = payNow ? StringUtils.defaultIfBlank(bo.getPayMethod(), "manual") : bo.getPayMethod();
        int rows = baseMapper.insertOrder(orderId, tenantId, memberId,
            bo.getStoreId() == null ? planVo.getStoreId() : bo.getStoreId(),
            orderNo,
            "buy_card",
            orderStatus,
            payMethod,
            payableAmount,
            BigDecimal.ZERO,
            payableAmount,
            paidAmount,
            paidTime,
            bo.getRemark());
        if (rows <= 0) {
            throw new ServiceException("创建订单失败");
        }
        baseMapper.insertOrderItem(IdGeneratorUtil.nextLongId(),
            FitnessBizCodeUtils.defaultIfBlank(null, "ITM"),
            tenantId, orderId,
            "card_plan",
            planVo.getPlanId(),
            planVo.getPlanName(),
            1,
            payableAmount,
            payableAmount,
            BigDecimal.ZERO,
            payableAmount,
            null);
        if (payNow) {
            int totalTimes = (planVo.getTotalTimes() == null ? 0 : planVo.getTotalTimes())
                + (planVo.getGiftTimes() == null ? 0 : planVo.getGiftTimes());
            Long cardId = IdGeneratorUtil.nextLongId();
            Date activeTime = new Date();
            Date expireTime = planVo.getValidDays() == null || planVo.getValidDays() <= 0
                ? null : DateUtil.offsetDay(activeTime, planVo.getValidDays());
            baseMapper.insertMemberCard(cardId,
                tenantId,
                bo.getStoreId() == null ? planVo.getStoreId() : bo.getStoreId(),
                memberId,
                planVo,
                "CARD" + IdGeneratorUtil.nextId(),
                bo.getBindCoachId(),
                activeTime,
                expireTime,
                totalTimes,
                nullSafe(planVo.getStoredAmount()),
                "active",
                orderId);
        }
        return queryOrderById(orderId);
    }

    @Override
    public TableDataInfo<FitnessMiniVo.OrderVo> queryMyOrderList(PageQuery pageQuery) {
        List<FitnessMiniVo.OrderVo> list = defaultList(baseMapper.selectMyOrderList(FitnessSecurityUtils.getCurrentMemberId()));
        return TableDataInfo.build(list, pageQuery.build());
    }

    @Override
    public FitnessMiniVo.OrderVo queryOrderById(Long orderId) {
        Long memberId = FitnessSecurityUtils.getCurrentMemberId();
        FitnessMiniVo.OrderVo orderVo = baseMapper.selectOrderById(memberId, orderId);
        if (orderVo == null) {
            throw new ServiceException("订单不存在");
        }
        orderVo.setItemList(defaultList(baseMapper.selectOrderItemList(orderId)));
        return orderVo;
    }

    @Override
    public List<FitnessMiniVo.MemberCardVo> queryMyMemberCards() {
        return defaultList(baseMapper.selectMyMemberCardList(FitnessSecurityUtils.getCurrentMemberId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FitnessMiniVo.CheckinVo createCheckin(FitnessMiniBo.CheckinCreateBo bo) {
        Long memberId = FitnessSecurityUtils.getCurrentMemberId();
        FitnessMiniVo.ReservationActionVo actionVo = baseMapper.selectReservationAction(memberId, bo.getReservationId());
        if (actionVo == null) {
            throw new ServiceException("预约记录不存在");
        }
        if (!Objects.equals("reserved", actionVo.getReservationStatus())) {
            throw new ServiceException("当前预约状态不可签到");
        }
        FitnessMiniVo.CheckinVo existed = baseMapper.selectCheckinByReservationId(memberId, bo.getReservationId());
        if (existed != null) {
            fillCheckinStatus(existed);
            return existed;
        }
        Long cardId = bo.getCardId() == null ? actionVo.getCardId() : bo.getCardId();
        if (cardId != null) {
            FitnessMiniVo.MemberCardVo cardVo = requireUsableCard(memberId, cardId);
            if (StringUtils.equals(cardVo.getPlanType(), "time_card")) {
                throw new ServiceException("时间卡（月卡/年卡）无需核销");
            }
        }

        Date now = new Date();
        int rows = baseMapper.insertCheckin(IdGeneratorUtil.nextLongId(),
            FitnessBizCodeUtils.defaultIfBlank(null, "CHK"),
            actionVo.getTenantId() == null ? FitnessSecurityUtils.getCurrentTenantId() : actionVo.getTenantId(),
            actionVo.getStoreId(),
            actionVo.getReservationId(),
            actionVo.getScheduleId(),
            memberId,
            cardId,
            StringUtils.defaultIfBlank(bo.getCheckinType(), "normal"),
            now,
            bo.getRemark());
        if (rows <= 0) {
            throw new ServiceException("签到失败");
        }
        baseMapper.updateMemberVisitInfo(memberId, actionVo.getStoreId(), now);
        FitnessMiniVo.CheckinVo checkinVo = baseMapper.selectCheckinByReservationId(memberId, bo.getReservationId());
        if (checkinVo == null) {
            throw new ServiceException("签到结果查询失败");
        }
        fillCheckinStatus(checkinVo);
        return checkinVo;
    }

    @Override
    public FitnessMiniVo.CheckinVo queryCheckinStatus(Long reservationId) {
        Long memberId = FitnessSecurityUtils.getCurrentMemberId();
        FitnessMiniVo.CheckinVo checkinVo = baseMapper.selectCheckinByReservationId(memberId, reservationId);
        if (checkinVo == null) {
            FitnessMiniVo.ReservationVo reservationVo = baseMapper.selectReservationById(memberId, reservationId);
            if (reservationVo == null) {
                throw new ServiceException("预约记录不存在");
            }
            checkinVo = new FitnessMiniVo.CheckinVo();
            checkinVo.setReservationId(reservationId);
            checkinVo.setScheduleId(reservationVo.getScheduleId());
            checkinVo.setCourseName(reservationVo.getCourseName());
            checkinVo.setCoachName(reservationVo.getCoachName());
            checkinVo.setScheduleDate(reservationVo.getScheduleDate());
            checkinVo.setStartTime(reservationVo.getStartTime());
            checkinVo.setEndTime(reservationVo.getEndTime());
            checkinVo.setCardId(reservationVo.getCardId());
            checkinVo.setCardName(reservationVo.getCardName());
            checkinVo.setDerivedCheckinStatus(FitnessMobileConstants.CHECKIN_NOT_STARTED);
            return checkinVo;
        }
        fillCheckinStatus(checkinVo);
        return checkinVo;
    }

    @Override
    public TableDataInfo<FitnessMiniVo.CheckinVo> queryMyCheckinList(PageQuery pageQuery) {
        List<FitnessMiniVo.CheckinVo> list = defaultList(baseMapper.selectMyCheckinList(FitnessSecurityUtils.getCurrentMemberId()));
        list.forEach(this::fillCheckinStatus);
        return TableDataInfo.build(list, pageQuery.build());
    }

    @Override
    public FitnessMiniVo.MineSummaryVo queryMineSummary() {
        Long memberId = FitnessSecurityUtils.getCurrentMemberId();
        return fillMineSummary(baseMapper.selectMineSummary(memberId), requireMember(memberId));
    }

    @Override
    public FitnessMiniVo.ProfileVo queryProfile() {
        FitnessMiniVo.ProfileVo profileVo = baseMapper.selectMemberProfile(FitnessSecurityUtils.getCurrentMemberId());
        if (profileVo == null) {
            throw new ServiceException("会员资料不存在");
        }
        return profileVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(FitnessMiniBo.ProfileUpdateBo bo) {
        if (baseMapper.updateMemberProfile(FitnessSecurityUtils.getCurrentMemberId(), bo) <= 0) {
            throw new ServiceException("更新个人资料失败");
        }
    }

    private FitnessMiniVo.MemberInfoVo requireMember(Long memberId) {
        FitnessMiniVo.MemberInfoVo memberInfoVo = baseMapper.selectMemberById(memberId);
        if (memberInfoVo == null) {
            throw new ServiceException("会员不存在");
        }
        return memberInfoVo;
    }

    private FitnessMiniVo.MemberCardVo requireUsableCard(Long memberId, Long cardId) {
        FitnessMiniVo.MemberCardVo cardVo = baseMapper.selectUsableMemberCard(memberId, cardId);
        if (cardVo == null) {
            throw new ServiceException("会员卡不可用");
        }
        if (StringUtils.equals(cardVo.getPlanType(), "time_card")) {
            return cardVo;
        }
        boolean noTimes = cardVo.getRemainTimes() != null && cardVo.getRemainTimes() <= 0;
        boolean noBalance = cardVo.getBalanceAmount() != null && cardVo.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0;
        if (noTimes && noBalance) {
            throw new ServiceException("当前会员卡余额或剩余次数不足");
        }
        return cardVo;
    }


    private String resolveConsumeMode(FitnessMiniVo.MemberCardVo cardVo) {
        if (StringUtils.equals(cardVo.getPlanType(), "stored_card")) {
            return "amount";
        }
        if (StringUtils.equals(cardVo.getPlanType(), "time_card")) {
            return "free";
        }
        return "times";
    }

    private void validateReservationCardRule(FitnessMiniVo.MemberCardVo cardVo, FitnessMiniVo.ReservationActionVo actionVo) {
        if (StringUtils.equals(cardVo.getPlanType(), "time_card")) {
            throw new ServiceException("时间卡（月卡/年卡）无需预约和核销，请直接到店上课");
        }
        if (StringUtils.equalsAny(actionVo.getCourseType(), "private", "small")
            && cardVo.getBindCoachId() != null
            && !Objects.equals(cardVo.getBindCoachId(), actionVo.getCoachId())) {
            throw new ServiceException("私教/小班预约请使用绑定教练对应课程时段");
        }
    }


    private void fillScheduleCheckinStatus(FitnessMiniVo.ScheduleVo vo) {
        vo.setDerivedCheckinStatus(deriveCheckinStatus(vo.getCheckinStatus(), vo.getReservationId(), null));
        vo.setReservedByMe(vo.getReservationId() != null);
        if (vo.getCapacity() != null && vo.getReservedCount() != null) {
            vo.setAvailableCount(Math.max(vo.getCapacity() - vo.getReservedCount(), 0));
        }
    }

    private void fillReservationCheckinStatus(FitnessMiniVo.ReservationVo vo) {
        vo.setDerivedCheckinStatus(deriveCheckinStatus(vo.getCheckinStatus(), vo.getReservationId(), vo.getOperatorUserId()));
    }

    private void fillCheckinStatus(FitnessMiniVo.CheckinVo vo) {
        vo.setDerivedCheckinStatus(deriveCheckinStatus(vo.getCheckinStatus(), vo.getReservationId(), vo.getOperatorUserId()));
    }

    private String deriveCheckinStatus(String checkinStatus, Long reservationId, Long operatorUserId) {
        if (reservationId == null) {
            return FitnessMobileConstants.CHECKIN_NOT_STARTED;
        }
        if (StringUtils.isBlank(checkinStatus)) {
            return FitnessMobileConstants.CHECKIN_NOT_STARTED;
        }
        if (Objects.equals("reversed", checkinStatus)) {
            return FitnessMobileConstants.CHECKIN_EXCEPTION;
        }
        if (operatorUserId != null) {
            return FitnessMobileConstants.CHECKIN_CONFIRMED;
        }
        return FitnessMobileConstants.CHECKIN_PENDING_CONFIRM;
    }

    private FitnessMiniVo.MineSummaryVo fillMineSummary(FitnessMiniVo.MineSummaryVo summaryVo,
                                                        FitnessMiniVo.MemberInfoVo memberInfoVo) {
        FitnessMiniVo.MineSummaryVo result = summaryVo == null ? new FitnessMiniVo.MineSummaryVo() : summaryVo;
        result.setMemberId(memberInfoVo.getMemberId());
        result.setRealName(memberInfoVo.getRealName());
        result.setNickname(memberInfoVo.getNickname());
        result.setMobile(memberInfoVo.getMobile());
        result.setAvatarUrl(memberInfoVo.getAvatarUrl());
        result.setTotalReservationCount(defaultLong(result.getTotalReservationCount()));
        result.setPendingReservationCount(defaultLong(result.getPendingReservationCount()));
        result.setTotalCardCount(defaultLong(result.getTotalCardCount()));
        result.setActiveCardCount(defaultLong(result.getActiveCardCount()));
        result.setPendingConfirmCount(defaultLong(result.getPendingConfirmCount()));
        result.setConfirmedCheckinCount(defaultLong(result.getConfirmedCheckinCount()));
        return result;
    }

    private List<String> buildBannerList(Long memberId, List<FitnessMiniVo.CourseVo> recommendCourseList) {
        List<String> result = new ArrayList<>();
        defaultList(baseMapper.selectStoreList(null, memberId)).stream()
            .map(FitnessMiniVo.StoreVo::getCoverUrl)
            .filter(StringUtils::isNotBlank)
            .forEach(result::add);
        recommendCourseList.stream()
            .map(FitnessMiniVo.CourseVo::getCoverUrl)
            .filter(StringUtils::isNotBlank)
            .forEach(result::add);
        return result.stream().distinct().limit(3).collect(Collectors.toList());
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

    private String buildDefaultNickname() {
        return "T1#" + buildLuckySuffix();
    }

    private boolean shouldRefreshDefaultNickname(String nickname) {
        return StringUtils.isBlank(nickname) || nickname.startsWith("微信用户");
    }


    private String buildLuckySuffix() {
        for (int i = 0; i < 64; i++) {
            String suffix = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (isGoodLuckySuffix(suffix)) {
                return suffix;
            }
        }
        return "6666";
    }

    private boolean isGoodLuckySuffix(String suffix) {
        if (BAD_LUCKY_SUFFIXES.contains(suffix)) {
            return false;
        }
        long badFourCount = suffix.chars().filter(ch -> ch == '4').count();
        return badFourCount < 3;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

