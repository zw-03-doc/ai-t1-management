package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.fitness.domain.FitOrder;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单视图对象 fit_order
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = FitOrder.class)
public class FitnessOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long orderId;

    @ExcelProperty("订单号")
    private String orderNo;

    private Long storeId;

    @ExcelProperty("门店")
    private String storeName;

    private Long memberId;

    @ExcelProperty("会员")
    private String memberName;

    @ExcelProperty("手机号")
    private String memberMobile;

    @ExcelProperty("订单类型")
    private String orderType;

    @ExcelProperty("订单来源")
    private String orderSource;

    @ExcelProperty("订单状态")
    private String orderStatus;

    @ExcelProperty("支付方式")
    private String payMethod;

    @ExcelProperty("订单总额")
    private BigDecimal totalAmount;

    @ExcelProperty("优惠金额")
    private BigDecimal discountAmount;

    @ExcelProperty("应付金额")
    private BigDecimal payableAmount;

    @ExcelProperty("实付金额")
    private BigDecimal paidAmount;

    @ExcelProperty("支付时间")
    private Date paidTime;

    private String tradeNo;

    private Long cashierUserId;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建时间")
    private Date createTime;

    private List<FitnessOrderItemVo> itemList;
}
