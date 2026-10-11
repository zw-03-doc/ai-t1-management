package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 教练/门店经营报表视图对象
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
public class FitnessCoachStoreReportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long storeId;

    @ExcelProperty("门店")
    private String storeName;

    private Long coachId;

    @ExcelProperty("教练")
    private String coachName;

    @ExcelProperty("排课数")
    private Long scheduleCount;

    @ExcelProperty("预约数")
    private Long reservationCount;

    @ExcelProperty("签到数")
    private Long checkinCount;

    @ExcelProperty("私教销售额")
    private BigDecimal privateCourseSalesAmount;
}
