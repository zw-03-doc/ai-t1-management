package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;


import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 卡项方案视图对象 fit_card_plan
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
public class FitnessCardPlanVo implements Serializable {


    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 卡项方案ID
     */
    @ExcelProperty(value = "方案ID")
    private Long planId;

    /**
     * 方案编号
     */
    @ExcelProperty(value = "方案编号")
    private String planNo;

    /**
     * 适用门店ID
     */
    private Long storeId;

    /**
     * 适用门店名称
     */
    @ExcelProperty(value = "适用门店")
    private String storeName;

    /**
     * 卡项名称
     */
    @ExcelProperty(value = "卡项名称")
    private String planName;

    /**
     * 卡项类型
     */
    @ExcelProperty(value = "卡项类型")
    private String planType;

    /**
     * 销售价
     */
    @ExcelProperty(value = "销售价")
    private BigDecimal salePrice;

    /**
     * 原价
     */
    @ExcelProperty(value = "原价")
    private BigDecimal originalPrice;

    /**
     * 有效天数
     */
    @ExcelProperty(value = "有效天数")
    private Integer validDays;

    /**
     * 总次数
     */
    @ExcelProperty(value = "总次数")
    private Integer totalTimes;

    /**
     * 赠送次数
     */
    @ExcelProperty(value = "赠送次数")
    private Integer giftTimes;

    /**
     * 储值金额
     */
    @ExcelProperty(value = "储值金额")
    private BigDecimal storedAmount;

    /**
     * 支持课程类型
     */
    @ExcelProperty(value = "支持课程类型")
    private String supportCourseType;

    /**
     * 是否绑定教练（0否 1是）
     */
    @ExcelProperty(value = "绑定教练")
    private String isPrivateBindCoach;

    /**
     * 绑定教练ID
     */
    private Long customCoachId;

    /**
     * 绑定教练名称
     */
    @ExcelProperty(value = "绑定教练")
    private String customCoachName;

    /**
     * 指定会员ID原始串
     */
    private String customMemberIdsRaw;

    /**
     * 指定会员ID列表
     */
    private List<Long> customMemberIds;

    /**
     * 指定会员名称
     */
    @ExcelProperty(value = "指定会员")
    private String customMemberNames;

    /**
     * 自动关联基础课程ID
     */
    private Long bindCourseId;

    /**
     * 自动关联基础课程名称
     */
    @ExcelProperty(value = "关联课程")
    private String bindCourseName;

    /**
     * 排序号
     */
    @ExcelProperty(value = "排序号")
    private Integer sortNo;

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
