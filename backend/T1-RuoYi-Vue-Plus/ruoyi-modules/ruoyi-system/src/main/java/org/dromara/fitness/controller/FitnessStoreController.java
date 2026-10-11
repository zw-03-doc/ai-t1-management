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
import org.dromara.fitness.domain.bo.FitnessStoreBo;
import org.dromara.fitness.domain.vo.FitnessStoreVo;
import org.dromara.fitness.service.IFitnessStoreService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 门店 Controller
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/store")
public class FitnessStoreController extends BaseController {

    private final IFitnessStoreService fitnessStoreService;

    @SaCheckPermission("fitness:store:list")
    @GetMapping("/list")
    public TableDataInfo<FitnessStoreVo> list(FitnessStoreBo bo, PageQuery pageQuery) {
        return fitnessStoreService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("fitness:store:export")
    @Log(title = "门店", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FitnessStoreBo bo, HttpServletResponse response) {
        List<FitnessStoreVo> list = fitnessStoreService.queryList(bo);
        ExcelUtil.exportExcel(list, "门店", FitnessStoreVo.class, response);
    }

    @SaCheckPermission("fitness:store:query")
    @GetMapping("/{storeId}")
    public R<FitnessStoreVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long storeId) {
        return R.ok(fitnessStoreService.queryById(storeId));
    }

    @SaCheckPermission("fitness:store:add")
    @Log(title = "门店", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FitnessStoreBo bo) {
        return toAjax(fitnessStoreService.insertByBo(bo));
    }

    @SaCheckPermission("fitness:store:edit")
    @Log(title = "门店", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FitnessStoreBo bo) {
        return toAjax(fitnessStoreService.updateByBo(bo));
    }

    @SaCheckPermission("fitness:store:edit")
    @Log(title = "门店", businessType = BusinessType.UPDATE)
    @PutMapping("/{storeId}/status")
    public R<Void> changeStatus(@PathVariable Long storeId, @RequestBody FitnessStoreBo bo) {
        return toAjax(fitnessStoreService.updateStatus(storeId, bo.getStatus()));
    }

    @SaCheckPermission("fitness:store:remove")
    @Log(title = "门店", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(fitnessStoreService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

}
