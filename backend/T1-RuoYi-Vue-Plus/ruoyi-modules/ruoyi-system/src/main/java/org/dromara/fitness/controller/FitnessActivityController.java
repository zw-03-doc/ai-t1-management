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
import org.dromara.fitness.domain.bo.FitnessActivityBo;
import org.dromara.fitness.domain.vo.FitnessActivityVo;
import org.dromara.fitness.service.IFitnessActivityService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 活动管理 Controller
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/activity")
public class FitnessActivityController extends BaseController {

    private final IFitnessActivityService fitnessActivityService;

    @SaCheckPermission("fitness:activity:list")
    @GetMapping("/list")
    public TableDataInfo<FitnessActivityVo> list(FitnessActivityBo bo, PageQuery pageQuery) {
        return fitnessActivityService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("fitness:activity:export")
    @Log(title = "活动管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FitnessActivityBo bo, HttpServletResponse response) {
        List<FitnessActivityVo> list = fitnessActivityService.queryList(bo);
        ExcelUtil.exportExcel(list, "活动管理", FitnessActivityVo.class, response);
    }

    @SaCheckPermission("fitness:activity:query")
    @GetMapping("/{activityId}")
    public R<FitnessActivityVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long activityId) {
        return R.ok(fitnessActivityService.queryById(activityId));
    }

    @SaCheckPermission("fitness:activity:add")
    @Log(title = "活动管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FitnessActivityBo bo) {
        return toAjax(fitnessActivityService.insertByBo(bo));
    }

    @SaCheckPermission("fitness:activity:edit")
    @Log(title = "活动管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FitnessActivityBo bo) {
        return toAjax(fitnessActivityService.updateByBo(bo));
    }

    @SaCheckPermission("fitness:activity:edit")
    @Log(title = "活动管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{activityId}/status")
    public R<Void> changeStatus(@PathVariable Long activityId, @RequestBody FitnessActivityBo bo) {
        return toAjax(fitnessActivityService.updateStatus(activityId, bo.getStatus()));
    }

    @SaCheckPermission("fitness:activity:remove")
    @Log(title = "活动管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(fitnessActivityService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
