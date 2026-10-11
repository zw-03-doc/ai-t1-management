package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 课程报表视图对象
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
public class FitnessCourseReportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long storeId;

    @ExcelProperty("门店")
    private String storeName;

    private Long courseId;

    @ExcelProperty("课程")
    private String courseName;

    @ExcelProperty("课程类型")
    private String courseType;

    @ExcelProperty("排课数")
    private Long scheduleCount;

    @ExcelProperty("预约数")
    private Long reservationCount;

    @ExcelProperty("签到数")
    private Long checkinCount;

    @ExcelProperty("出勤率")
    private BigDecimal attendanceRate;
}
