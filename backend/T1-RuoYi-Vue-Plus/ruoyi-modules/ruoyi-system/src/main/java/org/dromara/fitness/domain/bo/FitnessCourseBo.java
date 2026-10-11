package org.dromara.fitness.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.fitness.domain.FitCourse;

/**
 * 课程业务对象 fit_course
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = FitCourse.class, reverseConvertGenerate = false)
public class FitnessCourseBo extends BaseEntity {

    /**
     * 课程ID
     */
    @NotNull(message = "课程ID不能为空", groups = { EditGroup.class })
    private Long courseId;

    /**
     * 课程编号
     */
    private String courseNo;

    /**
     * 课程名称
     */

    @NotBlank(message = "课程名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String courseName;

    /**
     * 课程类型
     */
    private String courseType;

    /**
     * 归属门店ID
     */
    private Long storeId;

    /**
     * 默认教练ID
     */
    private Long defaultCoachId;

    /**
     * 课程时长（分钟）
     */
    private Integer durationMinutes;

    /**
     * 默认人数上限
     */
    private Integer capacity;

    /**
     * 课程封面图
     */
    private String coverUrl;

    /**
     * 课程介绍
     */
    private String description;

    /**
     * 支持卡项类型
     */
    private String supportCardType;

    /**
     * 状态（0上架 1下架）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
