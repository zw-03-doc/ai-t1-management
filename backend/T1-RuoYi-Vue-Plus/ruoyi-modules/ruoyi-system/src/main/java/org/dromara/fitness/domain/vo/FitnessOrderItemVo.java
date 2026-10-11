package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单明细视图对象 fit_order_item
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
public class FitnessOrderItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long itemId;

    @ExcelProperty("明细编号")
    private String itemNo;

    private Long orderId;

    @ExcelProperty("订单号")
    private String orderNo;

    @ExcelProperty("订单状态")
    private String orderStatus;

    private Long storeId;

    @ExcelProperty("门店")
    private String storeName;

    private Long memberId;

    @ExcelProperty("会员")
    private String memberName;

    @ExcelProperty("会员手机号")
    private String memberMobile;

    @ExcelProperty("明细类型")
    private String itemType;

    private Long itemRefId;

    @ExcelProperty("明细名称")
    private String itemName;

    @ExcelProperty("数量")
    private Integer quantity;

    @ExcelProperty("单价")
    private BigDecimal unitPrice;

    @ExcelProperty("小计金额")
    private BigDecimal totalPrice;

    @ExcelProperty("优惠金额")
    private BigDecimal discountAmount;

    @ExcelProperty("实付金额")
    private BigDecimal realAmount;

    @ExcelProperty("发放会员卡ID")
    private Long grantCardId;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建时间")
    private Date createTime;
}
