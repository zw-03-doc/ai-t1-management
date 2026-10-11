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
import org.dromara.fitness.domain.bo.FitnessMemberCardBo;
import org.dromara.fitness.domain.vo.FitnessMemberCardVo;
import org.dromara.fitness.service.IFitnessMemberCardService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会员持卡 Controller
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/memberCard")
public class FitnessMemberCardController extends BaseController {

    private final IFitnessMemberCardService fitnessMemberCardService;

    @SaCheckPermission("fitness:memberCard:list")
    @GetMapping("/list")
    public TableDataInfo<FitnessMemberCardVo> list(FitnessMemberCardBo bo, PageQuery pageQuery) {
        return fitnessMemberCardService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("fitness:memberCard:export")
    @Log(title = "会员持卡", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FitnessMemberCardBo bo, HttpServletResponse response) {
        List<FitnessMemberCardVo> list = fitnessMemberCardService.queryList(bo);
        ExcelUtil.exportExcel(list, "会员持卡", FitnessMemberCardVo.class, response);
    }

    @SaCheckPermission("fitness:memberCard:query")
    @GetMapping("/{cardId}")
    public R<FitnessMemberCardVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long cardId) {
        return R.ok(fitnessMemberCardService.queryById(cardId));
    }

    @SaCheckPermission("fitness:memberCard:manage")
    @Log(title = "会员持卡状态", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{cardId}/status")
    public R<Void> changeStatus(@PathVariable Long cardId, @RequestBody FitnessMemberCardBo bo) {
        return toAjax(fitnessMemberCardService.updateStatus(cardId, bo.getCardStatus()));
    }

    @SaCheckPermission("fitness:memberCard:manage")
    @Log(title = "会员持卡调整", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{cardId}/adjust")
    public R<Void> adjust(@PathVariable Long cardId, @RequestBody FitnessMemberCardBo bo) {
        return toAjax(fitnessMemberCardService.adjustRights(cardId, bo));
    }
}
