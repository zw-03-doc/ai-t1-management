package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import org.dromara.fitness.domain.FitCoach;

/**
 * 教练视图对象 fit_coach
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = FitCoach.class)
public class FitnessCoachVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 教练ID
     */
    @ExcelProperty(value = "教练ID")
    private Long coachId;

    /**
     * 教练编号
     */
    @ExcelProperty(value = "教练编号")
    private String coachNo;

    /**
     * 关联后台用户ID
     */

    private Long userId;

    /**
     * 后台账号
     */
    @ExcelProperty(value = "后台账号")
    private String userName;

    /**
     * 后台昵称
     */
    private String nickName;

    /**
     * 所属门店ID
     */
    private Long storeId;

    /**
     * 所属门店名称
     */
    @ExcelProperty(value = "所属门店")
    private String storeName;

    /**
     * 教练姓名
     */
    @ExcelProperty(value = "教练姓名")
    private String coachName;

    /**
     * 手机号
     */
    @ExcelProperty(value = "手机号")
    private String mobile;

    /**
     * 教练激活码
     */
    @ExcelProperty(value = "激活码")
    private String activationCode;

    /**
     * 性别（0未知 1男 2女）
     */
    @ExcelProperty(value = "性别")
    private String gender;

    /**
     * 头像地址
     */
    private String avatarUrl;

    /**
     * 擅长标签
     */
    @ExcelProperty(value = "擅长标签")
    private String skillTags;

    /**
     * 教练等级
     */
    @ExcelProperty(value = "教练等级")
    private String levelCode;

    /**
     * 教练简介
     */
    private String intro;

    /**
     * 任职状态（0在职 1停用）
     */
    @ExcelProperty(value = "任职状态")
    private String hireStatus;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;


    private String contactWechat;
}
