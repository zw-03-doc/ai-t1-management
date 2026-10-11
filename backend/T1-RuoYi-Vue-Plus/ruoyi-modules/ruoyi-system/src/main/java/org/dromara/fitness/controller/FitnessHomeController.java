package org.dromara.fitness.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.fitness.domain.bo.FitnessHomeBo;
import org.dromara.fitness.domain.vo.FitnessHomeOverviewVo;
import org.dromara.fitness.service.IFitnessHomeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健身房 PC 首页控制器
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/home")
public class FitnessHomeController extends BaseController {

    private final IFitnessHomeService fitnessHomeService;

    /**
     * 查询首页聚合数据
     */
    @SaCheckPermission("fitness:home:view")
    @GetMapping("/overview")
    public R<FitnessHomeOverviewVo> getOverview(FitnessHomeBo bo) {
        return R.ok(fitnessHomeService.queryOverview(bo));
    }
}
