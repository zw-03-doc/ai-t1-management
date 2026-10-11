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
import org.dromara.fitness.domain.bo.FitnessCoachAppBo;
import org.dromara.fitness.domain.vo.FitnessCoachAppVo;
import org.dromara.fitness.service.IFitnessCoachAppService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 教练 App 控制器
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/coachApp")
public class FitnessCoachAppController extends BaseController {

    private final IFitnessCoachAppService fitnessCoachAppService;

    @SaIgnore
    @PostMapping("/auth/login")
    public R<FitnessCoachAppVo.LoginVo> login(@Validated @RequestBody FitnessCoachAppBo.LoginBo bo) {
        return R.ok(fitnessCoachAppService.login(bo));
    }

    @SaIgnore
    @PostMapping("/auth/wechatLogin")
    public R<FitnessCoachAppVo.LoginVo> wechatLogin(@Validated @RequestBody FitnessCoachAppBo.WechatLoginBo bo) {
        return R.ok(fitnessCoachAppService.wechatLogin(bo));
    }

    @SaIgnore
    @PostMapping("/auth/wechatActivate")
    public R<FitnessCoachAppVo.LoginVo> wechatActivate(@Validated @RequestBody FitnessCoachAppBo.WechatActivateBo bo) {
        return R.ok(fitnessCoachAppService.wechatActivate(bo));
    }

    @SaCheckLogin
    @PostMapping("/auth/logout")
    public R<Void> logout() {
        fitnessCoachAppService.logout();
        return R.ok();
    }

    @SaCheckLogin
    @GetMapping("/home/overview")
    public R<FitnessCoachAppVo.HomeOverviewVo> homeOverview() {
        return R.ok(fitnessCoachAppService.queryHomeOverview());
    }

    @SaCheckLogin
    @GetMapping("/schedule/my")
    public TableDataInfo<FitnessCoachAppVo.ScheduleVo> mySchedule(FitnessCoachAppBo.ScheduleQueryBo bo, PageQuery pageQuery) {
        return fitnessCoachAppService.queryMySchedule(bo, pageQuery);
    }

    @SaCheckLogin
    @GetMapping("/schedule/{scheduleId}")
    public R<FitnessCoachAppVo.ScheduleVo> scheduleDetail(@PathVariable Long scheduleId) {
        return R.ok(fitnessCoachAppService.queryScheduleDetail(scheduleId));
    }

    @SaCheckLogin
    @GetMapping("/checkin/pending")
    public TableDataInfo<FitnessCoachAppVo.CheckinVo> checkinList(FitnessCoachAppBo.CheckinQueryBo bo, PageQuery pageQuery) {
        return fitnessCoachAppService.queryCheckinList(bo, pageQuery);
    }

    @SaCheckLogin
    @GetMapping("/checkin/{reservationId}")
    public R<FitnessCoachAppVo.CheckinVo> checkinDetail(@PathVariable Long reservationId) {
        return R.ok(fitnessCoachAppService.queryCheckinDetail(reservationId));
    }

    @SaCheckLogin
    @RepeatSubmit
    @RateLimiter(time = 1, count = 5, message = "操作过于频繁，请稍后再试")
    @PostMapping("/checkin/confirm")
    public R<FitnessCoachAppVo.CheckinVo> confirmCheckin(@Validated @RequestBody FitnessCoachAppBo.CheckinConfirmBo bo) {
        return R.ok(fitnessCoachAppService.confirmCheckin(bo));
    }

    @SaCheckLogin
    @RepeatSubmit
    @RateLimiter(time = 1, count = 5, message = "操作过于频繁，请稍后再试")
    @PostMapping("/checkin/exception")
    public R<FitnessCoachAppVo.CheckinVo> markException(@Validated @RequestBody FitnessCoachAppBo.CheckinExceptionBo bo) {
        return R.ok(fitnessCoachAppService.markCheckinException(bo));
    }

    @SaCheckLogin
    @GetMapping("/student/my")
    public TableDataInfo<FitnessCoachAppVo.StudentVo> myStudents(@RequestParam(required = false) String keyword, PageQuery pageQuery) {
        return fitnessCoachAppService.queryMyStudents(keyword, pageQuery);
    }

    @SaCheckLogin
    @GetMapping("/student/{memberId}")
    public R<FitnessCoachAppVo.StudentVo> studentDetail(@PathVariable Long memberId) {
        return R.ok(fitnessCoachAppService.queryStudentDetail(memberId));
    }

    @SaCheckLogin
    @GetMapping("/mine/profile")
    public R<FitnessCoachAppVo.CoachProfileVo> profile() {
        return R.ok(fitnessCoachAppService.queryProfile());
    }

    @SaCheckLogin
    @RepeatSubmit
    @RateLimiter(time = 1, count = 3, message = "操作过于频繁，请稍后再试")
    @PutMapping("/mine/profile")
    public R<Void> updateProfile(@RequestBody FitnessCoachAppBo.ProfileUpdateBo bo) {
        fitnessCoachAppService.updateProfile(bo);
        return R.ok();
    }

    @SaCheckLogin
    @RepeatSubmit
    @RateLimiter(time = 1, count = 3, message = "操作过于频繁，请稍后再试")
    @PutMapping("/mine/password")
    public R<Void> updatePassword(@Validated @RequestBody FitnessCoachAppBo.PasswordUpdateBo bo) {
        fitnessCoachAppService.updatePassword(bo);
        return R.ok();
    }

    @SaCheckLogin
    @GetMapping("/message/list")
    public TableDataInfo<FitnessCoachAppVo.MessageVo> messageList(PageQuery pageQuery) {
        return fitnessCoachAppService.queryMessageList(pageQuery);
    }
}
