package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.fitness.domain.FitCheckinRecord;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 签到核销视图对象 fit_checkin_record
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = FitCheckinRecord.class)
public class FitnessCheckinVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("签到ID")
    private Long checkinId;

    @ExcelProperty("签到编号")
    private String checkinNo;

    private Long reservationId;

    @ExcelProperty("预约编号")
    private String reservationNo;

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

    private Long memberId;

    private String memberNo;

    @ExcelProperty("会员")
    private String memberName;


    @ExcelProperty("手机号")
    private String memberMobile;

    private Long cardId;

    @ExcelProperty("预约类型")
    private String reservationType;

    @ExcelProperty("扣减卡项")
    private String cardName;

    @ExcelProperty("卡项类型")
    private String planType;

    private String cardStatus;


    private Integer remainTimes;

    private BigDecimal balanceAmount;

    @ExcelProperty("排课日期")
    private Date scheduleDate;

    @ExcelProperty("开始时间")
    private Date startTime;

    @ExcelProperty("结束时间")
    private Date endTime;

    @ExcelProperty("签到类型")
    private String checkinType;

    @ExcelProperty("签到时间")
    private Date checkinTime;

    @ExcelProperty("消耗次数")
    private Integer consumeTimes;

    @ExcelProperty("消耗金额")
    private BigDecimal consumeAmount;

    private Long operatorUserId;

    @ExcelProperty("核销人")
    private String operatorUserName;

    @ExcelProperty("签到状态")
    private String checkinStatus;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建时间")
    private Date createTime;
}
