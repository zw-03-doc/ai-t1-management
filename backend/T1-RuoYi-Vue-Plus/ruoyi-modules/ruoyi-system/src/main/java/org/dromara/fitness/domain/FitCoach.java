package org.dromara.fitness.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

/**
 * 教练实体对象 fit_coach
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fit_coach")
public class FitCoach extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 教练ID
     */
    @TableId(value = "coach_id")
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
    private String coachName;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 教练激活码
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


    private String contactWechat;
}
