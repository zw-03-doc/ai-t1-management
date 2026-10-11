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
import org.dromara.fitness.domain.bo.FitnessScheduleBo;
import org.dromara.fitness.domain.vo.FitnessScheduleVo;
import org.dromara.fitness.service.IFitnessScheduleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 排课 Controller
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/schedule")
public class FitnessScheduleController extends BaseController {

    private final IFitnessScheduleService fitnessScheduleService;

    @SaCheckPermission("fitness:schedule:list")
    @GetMapping("/list")
    public TableDataInfo<FitnessScheduleVo> list(FitnessScheduleBo bo, PageQuery pageQuery) {
        return fitnessScheduleService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("fitness:schedule:export")
    @Log(title = "排课", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FitnessScheduleBo bo, HttpServletResponse response) {
        List<FitnessScheduleVo> list = fitnessScheduleService.queryList(bo);
        ExcelUtil.exportExcel(list, "排课", FitnessScheduleVo.class, response);
    }

    @SaCheckPermission("fitness:schedule:query")
    @GetMapping("/{scheduleId}")
    public R<FitnessScheduleVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long scheduleId) {
        return R.ok(fitnessScheduleService.queryById(scheduleId));
    }

    @SaCheckPermission("fitness:schedule:add")
    @Log(title = "排课", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FitnessScheduleBo bo) {
        return toAjax(fitnessScheduleService.insertByBo(bo));
    }

    @SaCheckPermission("fitness:schedule:edit")
    @Log(title = "排课", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FitnessScheduleBo bo) {
        return toAjax(fitnessScheduleService.updateByBo(bo));
    }

    @SaCheckPermission("fitness:schedule:edit")
    @Log(title = "排课", businessType = BusinessType.UPDATE)
    @PutMapping("/{scheduleId}/cancel")
    public R<Void> cancel(@PathVariable Long scheduleId, @RequestBody(required = false) FitnessScheduleBo bo) {
        return toAjax(fitnessScheduleService.cancelSchedule(scheduleId, bo == null ? null : bo.getCancelReason()));
    }

    @SaCheckPermission("fitness:schedule:remove")
    @Log(title = "排课", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(fitnessScheduleService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
