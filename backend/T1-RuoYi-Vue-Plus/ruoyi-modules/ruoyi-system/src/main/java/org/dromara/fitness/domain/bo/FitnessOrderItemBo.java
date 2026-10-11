package org.dromara.fitness.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 订单明细查询对象
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FitnessOrderItemBo extends BaseEntity {

    private Long itemId;

    private Long orderId;

    private String orderNo;

    private Long storeId;

    private Long memberId;

    private String itemType;

    private String itemName;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date orderCreateTimeStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date orderCreateTimeEnd;
}
