package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.fitness.domain.FitCourseReservation;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 预约视图对象 fit_course_reservation
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = FitCourseReservation.class)
public class FitnessReservationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("预约ID")
    private Long reservationId;

    @ExcelProperty("预约编号")
    private String reservationNo;

    private Long storeId;


    @ExcelProperty("门店")
    private String storeName;

    private Long scheduleId;

    @ExcelProperty("排课编号")
    private String scheduleNo;

    private Long courseId;


    @ExcelProperty("课程")
    private String courseName;

    @ExcelProperty("课程类型")
    private String courseType;

    private Long coachId;

    @ExcelProperty("教练")
    private String coachName;

    private Long memberId;

    @ExcelProperty("会员")
    private String memberName;

    @ExcelProperty("手机号")
    private String memberMobile;

    private Long cardId;

    @ExcelProperty("预约类型")
    private String reservationType;

    @ExcelProperty("扣课卡项")
    private String cardName;

    @ExcelProperty("卡项类型")
    private String planType;


    @ExcelProperty("排课日期")
    private Date scheduleDate;

    @ExcelProperty("开始时间")
    private Date startTime;

    @ExcelProperty("结束时间")
    private Date endTime;

    @ExcelProperty("预约来源")
    private String reservationSource;

    @ExcelProperty("预约状态")
    private String reservationStatus;

    @ExcelProperty("预约时间")
    private Date reservationTime;

    private Date cancelTime;

    @ExcelProperty("取消原因")
    private String cancelReason;

    @ExcelProperty("扣费模式")
    private String consumeMode;

    private Long checkinId;

    @ExcelProperty("签到时间")
    private Date checkinTime;

    @ExcelProperty("签到状态")
    private String checkinStatus;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建时间")
    private Date createTime;
}
