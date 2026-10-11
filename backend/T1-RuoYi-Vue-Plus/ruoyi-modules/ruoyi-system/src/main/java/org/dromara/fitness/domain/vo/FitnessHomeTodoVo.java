package org.dromara.fitness.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 健身房 PC 首页待办视图对象
 *
 * @author CodeBuddy
 */
@Data
public class FitnessHomeTodoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 待办编码 */
    private String todoCode;

    /** 待办名称 */
    private String todoName;

    /** 待办数量 */
    private Long todoCount;

    /** 跳转路由 */
    private String route;
}
