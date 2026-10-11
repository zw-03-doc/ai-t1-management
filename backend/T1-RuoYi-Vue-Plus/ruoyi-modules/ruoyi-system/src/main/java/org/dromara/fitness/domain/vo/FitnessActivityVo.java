package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.fitness.domain.FitActivity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * 活动视图对象 fit_activity
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = FitActivity.class)
public class FitnessActivityVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "活动ID")
    private Long activityId;

    @ExcelProperty(value = "活动编号")
    private String activityNo;

    private Long storeId;

    @ExcelProperty(value = "门店")
    private String storeName;

    @ExcelProperty(value = "活动标题")
    private String title;

    @ExcelProperty(value = "活动副标题")
    private String subTitle;

    @ExcelProperty(value = "价格亮点")
    private String priceHighlight;

    @ExcelProperty(value = "适用人群")
    private String targetAudience;

    private String coverUrl;

    private String detailImageUrl;

    private String detailContent;

    @ExcelProperty(value = "开始时间")
    private Date startTime;

    @ExcelProperty(value = "结束时间")
    private Date endTime;

    @ExcelProperty(value = "倒计时截止")
    private Date countdownEndTime;

    @ExcelProperty(value = "本周主推")
    private String weeklyFeatured;

    @ExcelProperty(value = "活动类型")
    private String activityType;

    @ExcelProperty(value = "动作类型")
    private String actionType;

    @ExcelProperty(value = "折扣标签")
    private String discountLabel;

    @ExcelProperty(value = "规则备注")
    private String ruleNote;

    private List<Long> planIds;


    @ExcelProperty(value = "排序号")
    private Integer sortNo;

    @ExcelProperty(value = "状态")
    private String status;

    private String remark;

    @ExcelProperty(value = "创建时间")
    private Date createTime;
}
