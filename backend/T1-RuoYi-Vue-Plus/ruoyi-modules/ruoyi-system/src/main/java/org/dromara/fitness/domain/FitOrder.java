package org.dromara.fitness.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单实体对象 fit_order
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fit_order")
public class FitOrder extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "order_id")
    private Long orderId;

    private Long storeId;

    private String orderNo;

    private Long memberId;

    private String orderType;

    private String orderSource;

    private String orderStatus;

    private String payMethod;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    private BigDecimal paidAmount;

    private Date paidTime;

    private String tradeNo;

    private Long cashierUserId;

    private String remark;

    @Version
    private Integer version;

    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
