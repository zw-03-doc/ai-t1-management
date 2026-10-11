package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单报表视图对象
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
public class FitnessOrderReportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long storeId;

    @ExcelProperty("门店")
    private String storeName;

    @ExcelProperty("订单类型")
    private String orderType;

    @ExcelProperty("订单数")
    private Long orderCount;

    @ExcelProperty("支付单数")
    private Long paidOrderCount;

    @ExcelProperty("退款单数")
    private Long refundOrderCount;

    @ExcelProperty("应收金额")
    private BigDecimal payableAmount;

    @ExcelProperty("实收金额")
    private BigDecimal paidAmount;

    @ExcelProperty("退款金额")
    private BigDecimal refundAmount;
}
