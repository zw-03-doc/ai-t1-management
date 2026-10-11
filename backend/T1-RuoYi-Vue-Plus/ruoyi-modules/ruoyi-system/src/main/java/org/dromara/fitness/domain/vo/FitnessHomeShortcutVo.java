package org.dromara.fitness.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 健身房 PC 首页快捷入口视图对象
 *
 * @author CodeBuddy
 */
@Data
public class FitnessHomeShortcutVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 快捷入口编码 */
    private String shortcutCode;

    /** 快捷入口名称 */
    private String shortcutName;

    /** 跳转路由 */
    private String route;

    /** 按钮/页面权限 */
    private String permission;

    /** 图标 */
    private String icon;
}
