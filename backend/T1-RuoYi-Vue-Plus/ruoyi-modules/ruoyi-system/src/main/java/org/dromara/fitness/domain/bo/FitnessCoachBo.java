package org.dromara.fitness.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.fitness.domain.FitCoach;

/**
 * 教练业务对象 fit_coach
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = FitCoach.class, reverseConvertGenerate = false)
public class FitnessCoachBo extends BaseEntity {

    /**
     * 教练ID
     */
    @NotNull(message = "教练ID不能为空", groups = { EditGroup.class })
    private Long coachId;

    /**
     * 教练编号
     */
    private String coachNo;

    /**
     * 关联后台用户ID
     */

    private Long userId;

    /**
     * 所属门店ID
     */
    private Long storeId;

    /**
     * 教练姓名
     */
    @NotBlank(message = "教练姓名不能为空", groups = { AddGroup.class, EditGroup.class })
    private String coachName;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空", groups = { AddGroup.class, EditGroup.class })
    private String mobile;

    /**
     * 教练激活码（可手动填写，不填则自动生成）
     */
    private String activationCode;


    /**
     * 性别（0未知 1男 2女）
     */
    private String gender;

    /**
     * 头像地址
     */
    private String avatarUrl;

    /**
     * 擅长标签
     */
    private String skillTags;

    /**
     * 教练等级
     */
    private String levelCode;

    /**
     * 教练简介
     */
    private String intro;

    /**
     * 任职状态（0在职 1停用）
     */
    private String hireStatus;

    /**
     * 备注
     */
    private String remark;


    @jakarta.validation.constraints.Size(max = 100)
    private String contactWechat;
}
