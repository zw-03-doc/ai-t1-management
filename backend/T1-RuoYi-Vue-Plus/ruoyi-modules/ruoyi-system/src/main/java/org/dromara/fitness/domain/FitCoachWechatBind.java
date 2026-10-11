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
 * 教练微信绑定实体对象 fit_coach_wechat_bind
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fit_coach_wechat_bind")
public class FitCoachWechatBind extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 绑定ID
     */
    @TableId(value = "bind_id")
    private Long bindId;

    /**
     * 教练ID
     */
    private Long coachId;

    /**
     * 教练手机号
     */
    private String coachMobile;

    /**
     * 教练激活码
     */
    private String activationCode;

    /**
     * 教练微信openid
     */
    private String wechatOpenid;

    /**
     * 激活时间
     */
    private Date activateTime;

    /**
     * 绑定状态（0正常 1停用）
     */
    private String bindStatus;

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
