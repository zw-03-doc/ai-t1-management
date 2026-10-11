package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会员报表视图对象
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
public class FitnessMemberReportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long storeId;

    @ExcelProperty("门店")
    private String storeName;

    @ExcelProperty("新增会员数")
    private Long newMemberCount;

    @ExcelProperty("活跃会员数")
    private Long activeMemberCount;

    @ExcelProperty("付费会员数")
    private Long paidMemberCount;

    @ExcelProperty("有效会员卡数")
    private Long activeCardCount;

    @ExcelProperty("即将到期卡数")
    private Long expiringCardCount;
}
