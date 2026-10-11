package org.dromara.fitness.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.fitness.domain.FitMemberCard;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 会员持卡业务对象 fit_member_card
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = FitMemberCard.class, reverseConvertGenerate = false)
public class FitnessMemberCardBo extends BaseEntity {

    private Long cardId;

    private Long storeId;

    private Long memberId;

    private Long planId;

    private Long orderId;

    private Long bindCoachId;

    private String keyword;

    private String cardNo;

    private String cardName;

    private String planType;

    private String cardStatus;

    private Date activeTimeStart;

    private Date activeTimeEnd;

    private Date expireTimeStart;

    private Date expireTimeEnd;

    private Integer totalTimesAdjust;

    private Integer remainTimesAdjust;

    private BigDecimal balanceAmountAdjust;

    private Date expireTime;

    private String remark;
}
