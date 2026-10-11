package org.dromara.fitness.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serial;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

/**
 * 卡项方案实体对象 fit_card_plan
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fit_card_plan")
public class FitCardPlan extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 卡项方案ID
     */
    @TableId(value = "plan_id")
    private Long planId;

    /**
     * 方案编号
     */
    private String planNo;

    /**
     * 适用门店ID
     */

    private Long storeId;

    /**
     * 卡项名称
     */
    private String planName;

    /**
     * 卡项类型
     */
    private String planType;

    /**
     * 销售价
     */
    private BigDecimal salePrice;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

    /**
     * 有效天数
     */
    private Integer validDays;

    /**
     * 总次数
     */
    private Integer totalTimes;

    /**
     * 赠送次数
     */
    private Integer giftTimes;

    /**
     * 储值金额
     */
    private BigDecimal storedAmount;

    /**
     * 支持课程类型
     */
    private String supportCourseType;

    /**
     * 是否绑定教练（0否 1是）
     */
    private String isPrivateBindCoach;

    /**
     * 私教/小班包绑定教练ID
     */
    private Long customCoachId;

    /**
     * 私教/小班包指定会员ID串（逗号分隔）
     */
    private String customMemberIds;

    /**
     * 自动关联基础课程ID
     */
    private Long bindCourseId;

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
