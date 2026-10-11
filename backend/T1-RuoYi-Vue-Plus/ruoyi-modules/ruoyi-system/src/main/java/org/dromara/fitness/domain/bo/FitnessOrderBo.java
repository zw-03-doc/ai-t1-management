package org.dromara.fitness.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.fitness.domain.FitOrder;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单业务对象 fit_order
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = FitOrder.class, reverseConvertGenerate = false)
public class FitnessOrderBo extends BaseEntity {

    @NotNull(message = "订单ID不能为空", groups = { EditGroup.class })
    private Long orderId;

    private Long storeId;

    @NotNull(message = "会员ID不能为空", groups = { AddGroup.class })
    private Long memberId;

    @NotNull(message = "卡项方案ID不能为空", groups = { AddGroup.class })
    private Long planId;

    private Long bindCoachId;

    private String orderNo;

    private String orderType;

    private String orderSource;

    private String orderStatus;

    private String payMethod;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    private BigDecimal paidAmount;

    /**
     * 退款金额；当前业务策略仅支持等于实付金额的全额退款
     */
    private BigDecimal refundAmount;

    private String tradeNo;


    private Long cashierUserId;

    private Boolean payNow;

    private Date orderCreateTimeStart;

    private Date orderCreateTimeEnd;

    private String remark;
}
