package org.dromara.fitness.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.fitness.domain.bo.FitnessCheckinBo;
import org.dromara.fitness.domain.vo.FitnessCheckinVo;
import org.dromara.fitness.service.IFitnessCheckinService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 签到核销 Controller
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/checkin")
public class FitnessCheckinController extends BaseController {

    private final IFitnessCheckinService fitnessCheckinService;

    @SaCheckPermission("fitness:checkin:list")
    @GetMapping("/list")
    public TableDataInfo<FitnessCheckinVo> list(FitnessCheckinBo bo, PageQuery pageQuery) {
        return fitnessCheckinService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("fitness:checkin:export")
    @Log(title = "签到核销", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FitnessCheckinBo bo, HttpServletResponse response) {
        List<FitnessCheckinVo> list = fitnessCheckinService.queryList(bo);
        ExcelUtil.exportExcel(list, "签到核销", FitnessCheckinVo.class, response);
    }

    @SaCheckPermission("fitness:checkin:query")
    @GetMapping("/{checkinId}")
    public R<FitnessCheckinVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long checkinId) {
        return R.ok(fitnessCheckinService.queryById(checkinId));
    }

    @SaCheckPermission("fitness:checkin:edit")
    @Log(title = "签到确认", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{checkinId}/confirm")
    public R<Void> confirm(@PathVariable Long checkinId, @RequestBody(required = false) FitnessCheckinBo bo) {
        return toAjax(fitnessCheckinService.confirmCheckin(checkinId, bo == null ? new FitnessCheckinBo() : bo));
    }

    @SaCheckPermission("fitness:checkin:edit")
    @Log(title = "签到异常", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{checkinId}/exception")
    public R<Void> exception(@PathVariable Long checkinId, @RequestBody(required = false) FitnessCheckinBo bo) {
        return toAjax(fitnessCheckinService.markCheckinException(checkinId, bo == null ? null : bo.getRemark()));
    }
}
