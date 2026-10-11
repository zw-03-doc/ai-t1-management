package org.dromara.fitness.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 健身房 PC 首页查询对象
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FitnessHomeBo extends BaseEntity {

    /**
     * 门店ID，为空时统计当前租户全部门店
     */
    private Long storeId;
}
