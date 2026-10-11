package org.dromara.fitness.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.fitness.domain.bo.FitnessCardPlanBo;
import org.dromara.fitness.domain.vo.FitnessCardPlanVo;
import org.dromara.fitness.service.IFitnessCardPlanService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 卡项方案 Controller
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/cardPlan")
public class FitnessCardPlanController extends BaseController {

    private final IFitnessCardPlanService fitnessCardPlanService;

    @SaCheckPermission("fitness:cardPlan:list")
    @GetMapping("/list")
    public TableDataInfo<FitnessCardPlanVo> list(FitnessCardPlanBo bo, PageQuery pageQuery) {
        return fitnessCardPlanService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("fitness:cardPlan:export")
    @Log(title = "卡项方案", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FitnessCardPlanBo bo, HttpServletResponse response) {
        List<FitnessCardPlanVo> list = fitnessCardPlanService.queryList(bo);
        ExcelUtil.exportExcel(list, "卡项方案", FitnessCardPlanVo.class, response);
    }

    @SaCheckPermission("fitness:cardPlan:query")
    @GetMapping("/{planId}")
    public R<FitnessCardPlanVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long planId) {
        return R.ok(fitnessCardPlanService.queryById(planId));
    }

    @SaCheckPermission("fitness:cardPlan:add")
    @Log(title = "卡项方案", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FitnessCardPlanBo bo) {
        return toAjax(fitnessCardPlanService.insertByBo(bo));
    }

    @SaCheckPermission("fitness:cardPlan:edit")
    @Log(title = "卡项方案", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FitnessCardPlanBo bo) {
        return toAjax(fitnessCardPlanService.updateByBo(bo));
    }

    @SaCheckPermission("fitness:cardPlan:edit")
    @Log(title = "卡项方案", businessType = BusinessType.UPDATE)
    @PutMapping("/{planId}/status")
    public R<Void> changeStatus(@PathVariable Long planId, @RequestBody FitnessCardPlanBo bo) {
        return toAjax(fitnessCardPlanService.updateStatus(planId, bo.getStatus()));
    }

    @SaCheckPermission("fitness:cardPlan:remove")
    @Log(title = "卡项方案", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(fitnessCardPlanService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

}
