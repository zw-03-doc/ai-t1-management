package org.dromara.fitness.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.fitness.domain.FitCourseSchedule;

import java.util.Date;
import java.util.List;


/**
 * 排课业务对象 fit_course_schedule
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = FitCourseSchedule.class, reverseConvertGenerate = false)
public class FitnessScheduleBo extends BaseEntity {

    /**
     * 排课ID
     */
    @NotNull(message = "排课ID不能为空", groups = { EditGroup.class })
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
    @NotNull(message = "课程ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long courseId;

    /**
     * 授课教练ID
     */
    private Long coachId;

    /**
     * 排课日期
     */
    @NotNull(message = "排课日期不能为空", groups = { AddGroup.class, EditGroup.class })
    private Date scheduleDate;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空", groups = { AddGroup.class, EditGroup.class })
    private Date startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空", groups = { AddGroup.class, EditGroup.class })
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
     * 定向学员ID列表（私教/小班）
     */
    private List<Long> studentMemberIds;

    /**
     * 取消原因
     */

    private String cancelReason;

    /**
     * 查询开始日期
     */
    private Date scheduleDateStart;

    /**
     * 查询结束日期
     */
    private Date scheduleDateEnd;

    /**
     * 备注
     */
    private String remark;
}
