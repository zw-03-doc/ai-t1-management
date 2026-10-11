package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.fitness.domain.FitCourse;

/**
 * 课程视图对象 fit_course
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = FitCourse.class)
public class FitnessCourseVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 课程ID
     */
    @ExcelProperty(value = "课程ID")
    private Long courseId;

    /**
     * 课程编号
     */
    @ExcelProperty(value = "课程编号")
    private String courseNo;

    /**
     * 课程名称
     */

    @ExcelProperty(value = "课程名称")
    private String courseName;

    /**
     * 课程类型
     */
    @ExcelProperty(value = "课程类型")
    private String courseType;

    /**
     * 归属门店ID
     */
    private Long storeId;

    /**
     * 归属门店名称
     */
    @ExcelProperty(value = "归属门店")
    private String storeName;

    /**
     * 默认教练ID
     */
    private Long defaultCoachId;

    /**
     * 默认教练名称
     */
    @ExcelProperty(value = "默认教练")
    private String coachName;

    /**
     * 课程时长（分钟）
     */
    @ExcelProperty(value = "课程时长")
    private Integer durationMinutes;

    /**
     * 默认人数上限
     */
    @ExcelProperty(value = "人数上限")
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
    @ExcelProperty(value = "支持卡项类型")
    private String supportCardType;

    /**
     * 状态（0上架 1下架）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=正常,1=停用")
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}
