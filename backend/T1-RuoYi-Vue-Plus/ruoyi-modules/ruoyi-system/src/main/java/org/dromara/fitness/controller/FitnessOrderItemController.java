package org.dromara.fitness.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.fitness.domain.bo.FitnessOrderItemBo;
import org.dromara.fitness.domain.vo.FitnessOrderItemVo;
import org.dromara.fitness.service.IFitnessOrderItemService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单明细 Controller
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/orderItem")
public class FitnessOrderItemController extends BaseController {

    private final IFitnessOrderItemService fitnessOrderItemService;

    @SaCheckPermission("fitness:orderItem:list")
    @GetMapping("/list")
    public TableDataInfo<FitnessOrderItemVo> list(FitnessOrderItemBo bo, PageQuery pageQuery) {
        return fitnessOrderItemService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("fitness:orderItem:export")
    @Log(title = "订单明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FitnessOrderItemBo bo, HttpServletResponse response) {
        List<FitnessOrderItemVo> list = fitnessOrderItemService.queryList(bo);
        ExcelUtil.exportExcel(list, "订单明细", FitnessOrderItemVo.class, response);
    }

    @SaCheckPermission("fitness:orderItem:query")
    @GetMapping("/{itemId}")
    public R<FitnessOrderItemVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long itemId) {
        return R.ok(fitnessOrderItemService.queryById(itemId));
    }
}
