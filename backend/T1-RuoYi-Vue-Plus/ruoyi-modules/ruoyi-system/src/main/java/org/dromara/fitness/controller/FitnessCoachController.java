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
import org.dromara.fitness.domain.bo.FitnessCoachBo;
import org.dromara.fitness.domain.vo.FitnessCoachVo;
import org.dromara.fitness.service.IFitnessCoachService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 教练 Controller
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/coach")
public class FitnessCoachController extends BaseController {

    private final IFitnessCoachService fitnessCoachService;

    @SaCheckPermission("fitness:coach:list")
    @GetMapping("/list")
    public TableDataInfo<FitnessCoachVo> list(FitnessCoachBo bo, PageQuery pageQuery) {
        return fitnessCoachService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("fitness:coach:export")
    @Log(title = "教练", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FitnessCoachBo bo, HttpServletResponse response) {
        List<FitnessCoachVo> list = fitnessCoachService.queryList(bo);
        ExcelUtil.exportExcel(list, "教练", FitnessCoachVo.class, response);
    }

    @SaCheckPermission("fitness:coach:query")
    @GetMapping("/{coachId}")
    public R<FitnessCoachVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long coachId) {
        return R.ok(fitnessCoachService.queryById(coachId));
    }

    @SaCheckPermission("fitness:coach:add")
    @Log(title = "教练", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FitnessCoachBo bo) {
        return toAjax(fitnessCoachService.insertByBo(bo));
    }

    @SaCheckPermission("fitness:coach:edit")
    @Log(title = "教练", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FitnessCoachBo bo) {
        return toAjax(fitnessCoachService.updateByBo(bo));
    }

    @SaCheckPermission("fitness:coach:remove")
    @Log(title = "教练", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(fitnessCoachService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

}
