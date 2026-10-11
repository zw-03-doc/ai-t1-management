package org.dromara.fitness.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.web.core.BaseController;
import org.dromara.fitness.domain.bo.FitnessMiniBo;
import org.dromara.fitness.domain.vo.FitnessMiniVo;
import org.dromara.fitness.service.IFitnessMiniService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会员小程序控制器
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/mini")
public class FitnessMiniController extends BaseController {

    private final IFitnessMiniService fitnessMiniService;

    @SaIgnore
    @PostMapping("/auth/wechatLogin")
    public R<FitnessMiniVo.LoginVo> wechatLogin(@Validated @RequestBody FitnessMiniBo.WechatLoginBo bo) {
        return R.ok(fitnessMiniService.wechatLogin(bo));
    }

    @SaCheckLogin
    @RepeatSubmit
    @RateLimiter(time = 60, count = 1, message = "操作过于频繁，请稍后再试")
    @PostMapping("/auth/bindPhone")
    public R<Void> bindPhone(@Validated @RequestBody FitnessMiniBo.BindPhoneBo bo) {
        fitnessMiniService.bindPhone(bo);
        return R.ok();
    }

    @SaCheckLogin
    @GetMapping("/home/index")
    public R<FitnessMiniVo.HomeIndexVo> homeIndex() {
        return R.ok(fitnessMiniService.queryHomeIndex());
    }

    @SaCheckLogin
    @GetMapping("/store/list")
    public R<List<FitnessMiniVo.StoreVo>> storeList(@RequestParam(required = false) String keyword) {
        return R.ok(fitnessMiniService.queryStoreList(keyword));
    }

    @SaCheckLogin
    @GetMapping("/store/{storeId}")
    public R<FitnessMiniVo.StoreVo> storeDetail(@PathVariable Long storeId) {
        return R.ok(fitnessMiniService.queryStoreById(storeId));
    }

    @SaCheckLogin
    @GetMapping("/course/list")
    public R<List<FitnessMiniVo.CourseVo>> courseList(FitnessMiniBo.CourseQueryBo bo) {
        return R.ok(fitnessMiniService.queryCourseList(bo));
    }

    @SaCheckLogin
    @GetMapping("/course/{courseId}")
    public R<FitnessMiniVo.CourseVo> courseDetail(@PathVariable Long courseId) {
        return R.ok(fitnessMiniService.queryCourseById(courseId));
    }

    @SaCheckLogin
    @GetMapping("/schedule/list")
    public TableDataInfo<FitnessMiniVo.ScheduleVo> scheduleList(FitnessMiniBo.ScheduleQueryBo bo, PageQuery pageQuery) {
        return fitnessMiniService.queryScheduleList(bo, pageQuery);
    }

    @SaCheckLogin
    @RepeatSubmit
    @RateLimiter(time = 1, count = 5, message = "操作过于频繁，请稍后再试")
    @PostMapping("/reservation")
    public R<FitnessMiniVo.ReservationVo> createReservation(@Validated @RequestBody FitnessMiniBo.ReservationCreateBo bo) {
        return R.ok(fitnessMiniService.createReservation(bo));
    }

    @SaCheckLogin
    @GetMapping("/reservation/my")
    public TableDataInfo<FitnessMiniVo.ReservationVo> myReservation(PageQuery pageQuery) {
        return fitnessMiniService.queryMyReservationList(pageQuery);
    }

    @SaCheckLogin
    @RepeatSubmit
    @RateLimiter(time = 1, count = 5, message = "操作过于频繁，请稍后再试")
    @PutMapping("/reservation/{reservationId}/cancel")
    public R<Void> cancelReservation(@PathVariable Long reservationId) {
        fitnessMiniService.cancelReservation(reservationId);
        return R.ok();
    }

    @SaCheckLogin
    @GetMapping("/cardPlan/list")
    public R<List<FitnessMiniVo.CardPlanVo>> cardPlanList(@RequestParam(required = false) Long storeId) {
        return R.ok(fitnessMiniService.queryCardPlanList(storeId));
    }

    @SaCheckLogin
    @GetMapping("/cardPlan/{planId}")
    public R<FitnessMiniVo.CardPlanVo> cardPlanDetail(@PathVariable Long planId) {
        return R.ok(fitnessMiniService.queryCardPlanById(planId));
    }

    @SaCheckLogin
    @GetMapping("/activity/list")
    public R<List<FitnessMiniVo.ActivityVo>> activityList(@RequestParam(required = false) Long storeId) {
        return R.ok(fitnessMiniService.queryActivityList(storeId));
    }

    @SaCheckLogin
    @GetMapping("/activity/{activityId}")
    public R<FitnessMiniVo.ActivityVo> activityDetail(@PathVariable Long activityId) {
        return R.ok(fitnessMiniService.queryActivityById(activityId));
    }

    @SaCheckLogin
    @GetMapping("/activity/{activityId}/plans")
    public R<List<FitnessMiniVo.ActivityCardPlanVo>> activityCardPlans(@PathVariable Long activityId) {
        return R.ok(fitnessMiniService.queryActivityCardPlanList(activityId));
    }

    @SaCheckLogin
    @RepeatSubmit
    @RateLimiter(time = 1, count = 3, message = "操作过于频繁，请稍后再试")
    @PostMapping("/order")

    public R<FitnessMiniVo.OrderVo> createOrder(@Validated @RequestBody FitnessMiniBo.OrderCreateBo bo) {
        return R.ok(fitnessMiniService.createOrder(bo));
    }

    @SaCheckLogin
    @GetMapping("/order/my")
    public TableDataInfo<FitnessMiniVo.OrderVo> myOrder(PageQuery pageQuery) {
        return fitnessMiniService.queryMyOrderList(pageQuery);
    }

    @SaCheckLogin
    @GetMapping("/order/{orderId}")
    public R<FitnessMiniVo.OrderVo> orderDetail(@PathVariable Long orderId) {
        return R.ok(fitnessMiniService.queryOrderById(orderId));
    }

    @SaCheckLogin
    @GetMapping("/memberCard/my")
    public R<List<FitnessMiniVo.MemberCardVo>> myMemberCards() {
        return R.ok(fitnessMiniService.queryMyMemberCards());
    }

    @SaCheckLogin
    @RepeatSubmit
    @RateLimiter(time = 1, count = 3, message = "操作过于频繁，请稍后再试")
    @PostMapping("/checkin")
    public R<FitnessMiniVo.CheckinVo> createCheckin(@Validated @RequestBody FitnessMiniBo.CheckinCreateBo bo) {
        return R.ok(fitnessMiniService.createCheckin(bo));
    }

    @SaCheckLogin
    @GetMapping("/checkin/{reservationId}/status")
    public R<FitnessMiniVo.CheckinVo> checkinStatus(@PathVariable Long reservationId) {
        return R.ok(fitnessMiniService.queryCheckinStatus(reservationId));
    }

    @SaCheckLogin
    @GetMapping("/checkin/my")
    public TableDataInfo<FitnessMiniVo.CheckinVo> myCheckin(PageQuery pageQuery) {
        return fitnessMiniService.queryMyCheckinList(pageQuery);
    }

    @SaCheckLogin
    @GetMapping("/mine/summary")
    public R<FitnessMiniVo.MineSummaryVo> mineSummary() {
        return R.ok(fitnessMiniService.queryMineSummary());
    }

    @SaCheckLogin
    @GetMapping("/mine/profile")
    public R<FitnessMiniVo.ProfileVo> profile() {
        return R.ok(fitnessMiniService.queryProfile());
    }

    @SaCheckLogin
    @RepeatSubmit
    @PutMapping("/mine/profile")
    public R<Void> updateProfile(@RequestBody FitnessMiniBo.ProfileUpdateBo bo) {
        fitnessMiniService.updateProfile(bo);
        return R.ok();
    }
}
