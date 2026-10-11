package org.dromara.fitness.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.fitness.domain.FitActivity;

import java.util.Date;
import java.util.List;


/**
 * 活动业务对象 fit_activity
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = FitActivity.class, reverseConvertGenerate = false)
public class FitnessActivityBo extends BaseEntity {

    /**
     * 活动ID
     */
    @NotNull(message = "活动ID不能为空", groups = { EditGroup.class })
    private Long activityId;

    /**
     * 活动编号
     */
    private String activityNo;

    /**
     * 适用门店ID
     */
    private Long storeId;

    /**
     * 活动标题
     */
    @NotBlank(message = "活动标题不能为空", groups = { AddGroup.class, EditGroup.class })
    private String title;

    /**
     * 活动副标题
     */
    private String subTitle;

    /**
     * 价格亮点
     */
    private String priceHighlight;

    /**
     * 适用人群
     */
    private String targetAudience;

    /**
     * 活动封面图
     */
    @NotBlank(message = "活动封面图不能为空", groups = { AddGroup.class, EditGroup.class })
    private String coverUrl;

    /**
     * 活动详情图
     */
    private String detailImageUrl;

    /**
     * 活动详情文案
     */
    private String detailContent;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空", groups = { AddGroup.class, EditGroup.class })
    private Date startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空", groups = { AddGroup.class, EditGroup.class })
    private Date endTime;

    /**
     * 倒计时截止时间
     */
    private Date countdownEndTime;

    /**
     * 是否本周主推（0否 1是）
     */
    private String weeklyFeatured;

    /**
     * 活动类型（card_discount/custom_push）
     */
    @NotBlank(message = "活动类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String activityType;

    /**
     * 操作类型（detail/contact）
     */
    private String actionType;

    /**
     * 绑定卡项方案ID（仅优惠卡项活动使用）
     */
    private List<Long> planIds;

    /**
     * 折扣标签
     */
    private String discountLabel;

    /**
     * 规则备注
     */
    private String ruleNote;


    /**
     * 排序号
     */
    private Integer sortNo;

    /**
     * 状态（0上架 1下架）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
