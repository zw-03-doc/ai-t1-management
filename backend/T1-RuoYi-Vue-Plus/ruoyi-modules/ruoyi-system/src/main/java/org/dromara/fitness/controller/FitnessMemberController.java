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
import org.dromara.fitness.domain.bo.*;
import org.dromara.fitness.domain.vo.*;
import org.dromara.fitness.service.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 会员 Controller
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/member")
public class FitnessMemberController extends BaseController {

    private final IFitnessMemberService fitnessMemberService;
    private final IFitnessMemberCardService fitnessMemberCardService;
    private final IFitnessOrderService fitnessOrderService;
    private final IFitnessReservationService fitnessReservationService;
    private final IFitnessCheckinService fitnessCheckinService;

    @SaCheckPermission("fitness:member:list")
    @GetMapping("/list")
    public TableDataInfo<FitnessMemberVo> list(FitnessMemberBo bo, PageQuery pageQuery) {
        return fitnessMemberService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("fitness:member:export")
    @Log(title = "会员", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FitnessMemberBo bo, HttpServletResponse response) {
        List<FitnessMemberVo> list = fitnessMemberService.queryList(bo);
        ExcelUtil.exportExcel(list, "会员", FitnessMemberVo.class, response);
    }

    @SaCheckPermission("fitness:member:query")
    @GetMapping("/{memberId}")
    public R<FitnessMemberVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long memberId) {
        return R.ok(fitnessMemberService.queryById(memberId));
    }

    @SaCheckPermission("fitness:member:query")
    @GetMapping("/{memberId}/detail")
    public R<FitnessMemberDetailVo> getDetail(@NotNull(message = "主键不能为空") @PathVariable Long memberId) {
        FitnessMemberDetailVo vo = new FitnessMemberDetailVo();

        // 会员基本信息
        vo.setMember(fitnessMemberService.queryById(memberId));

        // 持卡权益
        FitnessMemberCardBo cardBo = new FitnessMemberCardBo();
        cardBo.setMemberId(memberId);
        List<FitnessMemberCardVo> cardList = fitnessMemberCardService.queryList(cardBo);
        vo.setCardList(cardList);
        vo.setCardCount(cardList.size());
        vo.setActiveCardCount((int) cardList.stream()
            .filter(c -> "active".equals(c.getCardStatus())).count());

        // 最近 10 条订单
        FitnessOrderBo orderBo = new FitnessOrderBo();
        orderBo.setMemberId(memberId);
        List<FitnessOrderVo> orderList = fitnessOrderService.queryList(orderBo);
        vo.setOrderList(orderList.stream().limit(10).toList());
        vo.setOrderCount(orderList.size());

        // 最近 10 条预约
        FitnessReservationBo reservationBo = new FitnessReservationBo();
        reservationBo.setMemberId(memberId);
        List<FitnessReservationVo> reservationList = fitnessReservationService.queryList(reservationBo);
        vo.setReservationList(reservationList.stream().limit(10).toList());
        vo.setReservationCount(reservationList.size());

        // 最近 10 条签到核销
        FitnessCheckinBo checkinBo = new FitnessCheckinBo();
        checkinBo.setMemberId(memberId);
        List<FitnessCheckinVo> checkinList = fitnessCheckinService.queryList(checkinBo);
        vo.setCheckinList(checkinList.stream().limit(10).toList());
        vo.setCheckinCount(checkinList.size());

        return R.ok(vo);
    }

    @SaCheckPermission("fitness:member:add")
    @Log(title = "会员", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FitnessMemberBo bo) {
        return toAjax(fitnessMemberService.insertByBo(bo));
    }

    @SaCheckPermission("fitness:member:edit")
    @Log(title = "会员", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FitnessMemberBo bo) {
        return toAjax(fitnessMemberService.updateByBo(bo));
    }

    @SaCheckPermission("fitness:member:remove")
    @Log(title = "会员", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(fitnessMemberService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

}
