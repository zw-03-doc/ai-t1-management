package org.dromara.fitness.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.fitness.domain.FitMember;

/**
 * 会员业务对象 fit_member
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = FitMember.class, reverseConvertGenerate = false)
public class FitnessMemberBo extends BaseEntity {

    /**
     * 会员ID
     */
    @NotNull(message = "会员ID不能为空", groups = { EditGroup.class })
    private Long memberId;

    /**
     * 会员编号
     */
    @NotBlank(message = "会员编号不能为空", groups = { AddGroup.class, EditGroup.class })
    private String memberNo;

    /**
     * 归属门店ID
     */
    private Long homeStoreId;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 性别（0未知 1男 2女）
     */
    private String gender;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 头像地址
     */
    private String avatarUrl;

    /**
     * 注册来源
     */
    private String registerSource;

    /**
     * 会员状态
     */
    private String memberStatus;

    /**
     * 最近消费门店ID
     */
    private Long lastStoreId;

    /**
     * 最近到店时间
     */
    private Date lastVisitTime;

    /**
     * 备注
     */
    private String remark;

}
