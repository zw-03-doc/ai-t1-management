package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.fitness.domain.FitCourseSchedule;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * 排课视图对象 fit_course_schedule
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = FitCourseSchedule.class)
public class FitnessScheduleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("排课ID")
    private Long scheduleId;

    @ExcelProperty("排课编号")
    private String scheduleNo;

    private Long storeId;


    @ExcelProperty("门店")
    private String storeName;

    private Long courseId;

    @ExcelProperty("课程")
    private String courseName;

    @ExcelProperty("课程类型")
    private String courseType;

    private Long coachId;

    @ExcelProperty("教练")
    private String coachName;

    @ExcelProperty("排课日期")
    private Date scheduleDate;

    @ExcelProperty("开始时间")
    private Date startTime;

    @ExcelProperty("结束时间")
    private Date endTime;

    @ExcelProperty("可预约人数")
    private Integer capacity;

    @ExcelProperty("已预约人数")
    private Integer reservedCount;

    @ExcelProperty("已签到人数")
    private Integer signedCount;

    private Date reservationDeadline;

    private Date cancelDeadline;

    @ExcelProperty("排课状态")
    private String scheduleStatus;

    @ExcelProperty("排课模式")
    private String scheduleMode;

    private List<Long> studentMemberIds;

    @ExcelProperty("学员")
    private String studentNames;

    @ExcelProperty("备注")
    private String remark;


    @ExcelProperty("创建时间")
    private Date createTime;

    /** Price captured when scheduling. */
    private java.math.BigDecimal displayPrice;
    private java.util.Date reservationOpenTime;
}
