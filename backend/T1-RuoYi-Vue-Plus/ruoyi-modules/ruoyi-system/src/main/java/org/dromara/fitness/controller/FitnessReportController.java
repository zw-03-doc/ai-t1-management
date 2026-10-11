package org.dromara.fitness.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.fitness.domain.bo.FitnessReportBo;
import org.dromara.fitness.domain.vo.FitnessCoachStoreReportVo;
import org.dromara.fitness.domain.vo.FitnessCourseReportVo;
import org.dromara.fitness.domain.vo.FitnessMemberReportVo;
import org.dromara.fitness.domain.vo.FitnessOrderReportVo;
import org.dromara.fitness.service.IFitnessReportService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报表中心 Controller
 *
 * @author CodeBuddy
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fitness/report")
public class FitnessReportController extends BaseController {

    private final IFitnessReportService fitnessReportService;

    @SaCheckPermission("fitness:report:list")
    @GetMapping("/member")
    public R<List<FitnessMemberReportVo>> member(FitnessReportBo bo) {
        return R.ok(fitnessReportService.queryMemberReportList(bo));
    }

    @SaCheckPermission("fitness:report:export")
    @Log(title = "会员报表", businessType = BusinessType.EXPORT)
    @PostMapping("/member/export")
    public void memberExport(FitnessReportBo bo, HttpServletResponse response) {
        ExcelUtil.exportExcel(fitnessReportService.queryMemberReportList(bo), "会员报表", FitnessMemberReportVo.class, response);
    }

    @SaCheckPermission("fitness:report:list")
    @GetMapping("/order")
    public R<List<FitnessOrderReportVo>> order(FitnessReportBo bo) {
        return R.ok(fitnessReportService.queryOrderReportList(bo));
    }

    @SaCheckPermission("fitness:report:export")
    @Log(title = "订单报表", businessType = BusinessType.EXPORT)
    @PostMapping("/order/export")
    public void orderExport(FitnessReportBo bo, HttpServletResponse response) {
        ExcelUtil.exportExcel(fitnessReportService.queryOrderReportList(bo), "订单报表", FitnessOrderReportVo.class, response);
    }

    @SaCheckPermission("fitness:report:list")
    @GetMapping("/course")
    public R<List<FitnessCourseReportVo>> course(FitnessReportBo bo) {
        return R.ok(fitnessReportService.queryCourseReportList(bo));
    }

    @SaCheckPermission("fitness:report:export")
    @Log(title = "课程报表", businessType = BusinessType.EXPORT)
    @PostMapping("/course/export")
    public void courseExport(FitnessReportBo bo, HttpServletResponse response) {
        ExcelUtil.exportExcel(fitnessReportService.queryCourseReportList(bo), "课程报表", FitnessCourseReportVo.class, response);
    }

    @SaCheckPermission("fitness:report:list")
    @GetMapping("/coachStore")
    public R<List<FitnessCoachStoreReportVo>> coachStore(FitnessReportBo bo) {
        return R.ok(fitnessReportService.queryCoachStoreReportList(bo));
    }

    @SaCheckPermission("fitness:report:export")
    @Log(title = "教练门店经营报表", businessType = BusinessType.EXPORT)
    @PostMapping("/coachStore/export")
    public void coachStoreExport(FitnessReportBo bo, HttpServletResponse response) {
        ExcelUtil.exportExcel(fitnessReportService.queryCoachStoreReportList(bo), "教练门店经营报表", FitnessCoachStoreReportVo.class, response);
    }
}
