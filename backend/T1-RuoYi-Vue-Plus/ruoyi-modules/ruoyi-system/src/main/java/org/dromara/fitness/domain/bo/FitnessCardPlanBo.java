package org.dromara.fitness.domain.bo;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;


import java.math.BigDecimal;
import java.util.List;

/**
 * 卡项方案业务对象 fit_card_plan
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FitnessCardPlanBo extends BaseEntity {


    /**
     * 卡项方案ID
     */
    @NotNull(message = "卡项方案ID不能为空", groups = { EditGroup.class })
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
    @NotBlank(message = "卡项名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String planName;

    /**
     * 卡项类型
     */
    @NotBlank(message = "卡项类型不能为空", groups = { AddGroup.class, EditGroup.class })
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
     * 私教/小班包指定会员ID列表（1-5名）
     */
    private List<Long> customMemberIds;

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

}
