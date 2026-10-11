package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.fitness.domain.FitMemberCard;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 会员持卡视图对象 fit_member_card
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = FitMemberCard.class)
public class FitnessMemberCardVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long cardId;

    private Long storeId;

    @ExcelProperty("门店")
    private String storeName;

    private Long memberId;

    @ExcelProperty("会员")
    private String memberName;

    @ExcelProperty("手机号")
    private String memberMobile;

    private Long planId;

    @ExcelProperty("方案名称")
    private String planName;

    @ExcelProperty("卡号")
    private String cardNo;

    @ExcelProperty("卡项名称")
    private String cardName;

    @ExcelProperty("卡项类型")
    private String planType;

    private Long bindCoachId;

    @ExcelProperty("绑定教练")
    private String coachName;

    @ExcelProperty("生效时间")
    private Date activeTime;

    @ExcelProperty("到期时间")
    private Date expireTime;

    @ExcelProperty("总次数")
    private Integer totalTimes;

    @ExcelProperty("已用次数")
    private Integer usedTimes;

    @ExcelProperty("剩余次数")
    private Integer remainTimes;

    @ExcelProperty("剩余余额")
    private BigDecimal balanceAmount;

    @ExcelProperty("卡状态")
    private String cardStatus;

    private Long orderId;

    @ExcelProperty("来源订单")
    private String orderNo;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建时间")
    private Date createTime;
}
