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
