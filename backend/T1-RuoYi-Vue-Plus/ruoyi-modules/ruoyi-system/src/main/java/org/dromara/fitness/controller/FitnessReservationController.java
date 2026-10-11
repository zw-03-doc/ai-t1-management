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
import org.dromara.fitness.domain.bo.FitnessReservationBo;
import org.dromara.fitness.domain.vo.FitnessReservationVo;
import org.dromara.fitness.service.IFitnessReservationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/reservation")
public class FitnessReservationController extends BaseController {

    private final IFitnessReservationService reservationService;

    @SaCheckPermission("fitness:reservation:list")
    @GetMapping("/list")
    public TableDataInfo<FitnessReservationVo> list(FitnessReservationBo bo, PageQuery pageQuery) {
        return reservationService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("fitness:reservation:export")
    @Log(title = "预约", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FitnessReservationBo bo, HttpServletResponse response) {
        List<FitnessReservationVo> list = reservationService.queryList(bo);
        ExcelUtil.exportExcel(list, "预约", FitnessReservationVo.class, response);
    }

    @SaCheckPermission("fitness:reservation:query")
    @GetMapping("/{reservationId}")
    public R<FitnessReservationVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long reservationId) {
        return R.ok(reservationService.queryById(reservationId));
    }

    @SaCheckPermission("fitness:reservation:add")
    @Log(title = "预约", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FitnessReservationBo bo) {
        return toAjax(reservationService.insertByBo(bo));
    }

    @SaCheckPermission("fitness:reservation:edit")
    @Log(title = "预约", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FitnessReservationBo bo) {
        return toAjax(reservationService.updateByBo(bo));
    }

    @SaCheckPermission("fitness:reservation:edit")
    @Log(title = "预约", businessType = BusinessType.UPDATE)
    @PutMapping("/{reservationId}/cancel")
    public R<Void> cancel(@PathVariable Long reservationId, @RequestBody(required = false) FitnessReservationBo bo) {
        return toAjax(reservationService.cancelReservation(reservationId, bo == null ? null : bo.getCancelReason()));
    }

    @SaCheckPermission("fitness:reservation:remove")
    @Log(title = "预约", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(reservationService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
