package org.dromara.fitness.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serial;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

/**
 * 会员实体对象 fit_member
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fit_member")
public class FitMember extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会员ID
     */
    @TableId(value = "member_id")
    private Long memberId;

    /**
     * 会员编号
     */
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
     * 微信openid
     */
    private String wxOpenid;

    /**
     * 微信unionid
     */
    private String wxUnionid;

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

}
