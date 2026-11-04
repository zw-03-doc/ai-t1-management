package org.dromara.fitness.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.fitness.domain.bo.FitnessEngagementBo;
import org.dromara.fitness.domain.vo.FitnessEngagementVo;
import org.dromara.fitness.service.support.FitnessOperationsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FitnessEngagementController {
    private final FitnessOperationsService operations;
    private final org.dromara.fitness.service.support.FitnessEntitlementService entitlements;

    @SaCheckLogin
    @GetMapping("/fitness/mini/member/entitlements/{scheduleId}")
    public R<java.util.List<org.dromara.fitness.domain.FitMemberCard>> eligibleCards(@PathVariable Long scheduleId) {
        return R.ok(entitlements.eligibleCards(org.dromara.fitness.utils.FitnessSecurityUtils.getCurrentMemberId(), scheduleId));
    }

    @SaCheckPermission("fitness:reservation:edit")
    @PutMapping("/fitness/reservation/{reservationId}/resolve")
    public R<Void> resolve(@PathVariable Long reservationId, @RequestBody org.dromara.fitness.domain.bo.FitnessReservationBo bo) {
        operations.resolveReservation(reservationId, bo.getReservationStatus(), bo.getRemark(), null);
        return R.ok();
    }

    @SaCheckPermission("fitness:checkin:edit")
    @PutMapping("/fitness/checkin/{checkinId}/restore")
    public R<Void> restore(@PathVariable Long checkinId, @RequestBody org.dromara.fitness.domain.bo.FitnessCheckinBo bo) {
        operations.restoreCheckin(checkinId, bo.getRemark(), null);
        return R.ok();
    }

    @SaCheckLogin
    @PostMapping("/fitness/coachApp/checkin/{checkinId}/restore")
    public R<Void> coachRestore(@PathVariable Long checkinId, @RequestBody org.dromara.fitness.domain.bo.FitnessCheckinBo bo) {
        operations.restoreCheckin(checkinId, bo.getRemark(), org.dromara.fitness.utils.FitnessSecurityUtils.getCurrentCoachId());
        return R.ok();
    }

    @SaCheckLogin
    @RateLimiter(key = "#{T(org.dromara.common.satoken.utils.LoginHelper).getUserId()}", time = 60, count = 30, message = "浏览记录提交过于频繁")
    @PostMapping("/fitness/mini/course/{courseId}/interest")
    public R<Void> interest(@PathVariable Long courseId, @Valid @RequestBody FitnessEngagementBo.Interest bo) {
        operations.recordInterest(courseId, bo);
        return R.ok();
    }

    @SaCheckLogin
    @RateLimiter(key = "#{T(org.dromara.common.satoken.utils.LoginHelper).getUserId()}", time = 60, count = 30, message = "操作过于频繁")
    @PostMapping("/fitness/coachApp/checkin/proxy")
    public R<Long> proxy(@Valid @RequestBody FitnessEngagementBo.ProxyCheckin bo) {
        return R.ok(operations.proxyCheckin(bo));
    }

    @SaCheckLogin
    @GetMapping("/fitness/coachApp/interest/list")
    public TableDataInfo<FitnessEngagementVo.Interest> interests(@RequestParam(required = false) Long memberId, PageQuery page) {
        return operations.interests(memberId, page);
    }

    @SaCheckPermission("fitness:behavior:list")
    @GetMapping("/fitness/behavior/list")
    public TableDataInfo<FitnessEngagementVo.Behavior> behaviors(@RequestParam(required = false) Long memberId,
        @RequestParam(required = false) String action, PageQuery page) {
        return operations.behaviors(memberId, action, page);
    }
}
