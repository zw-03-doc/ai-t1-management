package org.dromara.fitness.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 健身房 PC 首页聚合视图对象
 *
 * @author CodeBuddy
 */
@Data
public class FitnessHomeOverviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 首页统计数据
     */
    private FitnessHomeStatsVo stats;

    /**
     * 待办中心
     */
    private List<FitnessHomeTodoVo> todoList;

    /**
     * 快捷入口
     */
    private List<FitnessHomeShortcutVo> shortcutList;

    /**
     * 今日课程与现场节奏
     */
    private List<FitnessHomeScheduleVo> scheduleList;
}
