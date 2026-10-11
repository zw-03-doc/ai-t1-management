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
import org.dromara.fitness.domain.bo.FitnessOrderBo;
import org.dromara.fitness.domain.vo.FitnessOrderVo;
import org.dromara.fitness.service.IFitnessOrderService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 订单 Controller
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/order")
public class FitnessOrderController extends BaseController {

    private final IFitnessOrderService fitnessOrderService;

    @SaCheckPermission("fitness:order:list")
    @GetMapping("/list")
    public TableDataInfo<FitnessOrderVo> list(FitnessOrderBo bo, PageQuery pageQuery) {
        return fitnessOrderService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("fitness:order:export")
    @Log(title = "订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FitnessOrderBo bo, HttpServletResponse response) {
        List<FitnessOrderVo> list = fitnessOrderService.queryList(bo);
        ExcelUtil.exportExcel(list, "订单", FitnessOrderVo.class, response);
    }

    @SaCheckPermission("fitness:order:query")
    @GetMapping("/{orderId}")
    public R<FitnessOrderVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long orderId) {
        return R.ok(fitnessOrderService.queryById(orderId));
    }

    @SaCheckPermission("fitness:order:add")
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FitnessOrderBo bo) {
        return toAjax(fitnessOrderService.insertByBo(bo));
    }

    @SaCheckPermission("fitness:order:edit")
    @Log(title = "订单", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FitnessOrderBo bo) {
        return toAjax(fitnessOrderService.updateByBo(bo));
    }

    @SaCheckPermission("fitness:order:edit")
    @Log(title = "订单支付", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{orderId}/pay")
    public R<Void> pay(@PathVariable Long orderId, @RequestBody(required = false) FitnessOrderBo bo) {
        return toAjax(fitnessOrderService.payOrder(orderId, bo == null ? new FitnessOrderBo() : bo));
    }

    @SaCheckPermission("fitness:order:edit")
    @Log(title = "订单取消", businessType = BusinessType.UPDATE)
    @PutMapping("/{orderId}/cancel")
    public R<Void> cancel(@PathVariable Long orderId, @RequestBody(required = false) FitnessOrderBo bo) {
        return toAjax(fitnessOrderService.cancelOrder(orderId, bo == null ? null : bo.getRemark()));
    }

    @SaCheckPermission("fitness:order:edit")
    @Log(title = "订单退款", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{orderId}/refund")
    public R<Void> refund(@PathVariable Long orderId, @RequestBody(required = false) FitnessOrderBo bo) {
        return toAjax(fitnessOrderService.refundOrder(orderId, bo == null ? new FitnessOrderBo() : bo));
    }

    @SaCheckPermission("fitness:order:remove")

    @Log(title = "订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(fitnessOrderService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
