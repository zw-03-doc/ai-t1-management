package org.dromara.fitness.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 健身房 PC 首页今日课程节奏视图对象
 *
 * @author CodeBuddy
 */
@Data
public class FitnessHomeScheduleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 排课ID
     */
    private Long scheduleId;

    /**
     * 排课编号
     */
    private String scheduleNo;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 门店名称
     */
    private String storeName;

    /**
     * 教练名称
     */
    private String coachName;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 可预约人数
     */
    private Integer capacity;

    /**
     * 已预约人数
     */
    private Integer reservedCount;

    /**
     * 已签到人数
     */
    private Integer signedCount;

    /**
     * 排课状态
     */
    private String scheduleStatus;
}
