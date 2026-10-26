package org.dromara.fitness.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 排课实体对象 fit_course_schedule
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fit_course_schedule")
public class FitCourseSchedule extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 排课ID
     */
    @TableId(value = "schedule_id")
    private Long scheduleId;

    /**
     * 排课编号
     */
    private String scheduleNo;

    /**
     * 门店ID
     */

    private Long storeId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 授课教练ID
     */
    private Long coachId;

    /**
     * 排课日期
     */
    private Date scheduleDate;

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
     * 预约截止时间
     */
    private Date reservationDeadline;

    /**
     * 取消截止时间
     */
    private Date cancelDeadline;

    /**
     * 排课状态
     */
    private String scheduleStatus;

    /**
     * 排课模式（normal/private/small_group）
     */
    private String scheduleMode;

    /**
     * 备注
     */

    private String remark;

    /**
     * 版本号
     */
    @Version
    private Integer version;

    /**
     * 删除标记（0存在 2删除）
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;

    /** Price captured when scheduling. */
    private java.math.BigDecimal displayPrice;
    private java.util.Date reservationOpenTime;
}
