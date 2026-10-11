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

/**
 * 订单明细实体对象 fit_order_item
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fit_order_item")
public class FitOrderItem extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "item_id")
    private Long itemId;

    private String itemNo;

    private Long orderId;


    private String itemType;

    private Long itemRefId;

    private String itemName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private BigDecimal discountAmount;

    private BigDecimal realAmount;

    private Long grantCardId;

    private String remark;

    @Version
    private Integer version;

    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
