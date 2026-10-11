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
import org.dromara.fitness.domain.bo.FitnessCourseBo;
import org.dromara.fitness.domain.vo.FitnessCourseVo;
import org.dromara.fitness.service.IFitnessCourseService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 课程 Controller
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/course")
public class FitnessCourseController extends BaseController {

    private final IFitnessCourseService fitnessCourseService;

    @SaCheckPermission("fitness:course:list")
    @GetMapping("/list")
    public TableDataInfo<FitnessCourseVo> list(FitnessCourseBo bo, PageQuery pageQuery) {
        return fitnessCourseService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("fitness:course:export")
    @Log(title = "课程", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FitnessCourseBo bo, HttpServletResponse response) {
        List<FitnessCourseVo> list = fitnessCourseService.queryList(bo);
        ExcelUtil.exportExcel(list, "课程", FitnessCourseVo.class, response);
    }

    @SaCheckPermission("fitness:course:query")
    @GetMapping("/{courseId}")
    public R<FitnessCourseVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long courseId) {
        return R.ok(fitnessCourseService.queryById(courseId));
    }

    @SaCheckPermission("fitness:course:add")
    @Log(title = "课程", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FitnessCourseBo bo) {
        return toAjax(fitnessCourseService.insertByBo(bo));
    }

    @SaCheckPermission("fitness:course:edit")
    @Log(title = "课程", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FitnessCourseBo bo) {
        return toAjax(fitnessCourseService.updateByBo(bo));
    }

    @SaCheckPermission("fitness:course:edit")
    @Log(title = "课程", businessType = BusinessType.UPDATE)
    @PutMapping("/{courseId}/status")
    public R<Void> changeStatus(@PathVariable Long courseId, @RequestBody FitnessCourseBo bo) {
        return toAjax(fitnessCourseService.updateStatus(courseId, bo.getStatus()));
    }

    @SaCheckPermission("fitness:course:remove")
    @Log(title = "课程", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(fitnessCourseService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

}
