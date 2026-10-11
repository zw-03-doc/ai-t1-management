package org.dromara.fitness.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 活动实体对象 fit_activity
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fit_activity")
public class FitActivity extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 活动ID
     */
    @TableId(value = "activity_id")
    private Long activityId;

    /**
     * 活动编号
     */
    private String activityNo;

    /**
     * 适用门店ID（为空表示全门店）
     */
    private Long storeId;

    /**
     * 活动标题
     */
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
    private Date startTime;

    /**
     * 结束时间
     */
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
    private String activityType;

    /**
     * 操作类型（detail/contact）
     */
    private String actionType;

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
     * 版本号
     */
    @Version
    private Integer version;

    /**
     * 删除标记（0存在 2删除）
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
