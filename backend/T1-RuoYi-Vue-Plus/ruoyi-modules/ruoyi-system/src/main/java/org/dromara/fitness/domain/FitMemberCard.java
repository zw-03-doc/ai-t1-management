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
 * 会员卡实体对象 fit_member_card
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fit_member_card")
public class FitMemberCard extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "card_id")
    private Long cardId;

    private Long storeId;

    private Long memberId;

    private Long planId;

    private String cardNo;

    private String cardName;

    private String planType;

    private Long bindCoachId;

    private Date activeTime;

    private Date expireTime;

    private Integer totalTimes;

    private Integer usedTimes;

    private Integer remainTimes;

    private BigDecimal balanceAmount;

    private String cardStatus;

    private Long orderId;

    private String remark;

    @Version
    private Integer version;

    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
