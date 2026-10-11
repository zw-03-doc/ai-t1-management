package org.dromara.fitness.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import org.dromara.fitness.domain.FitMember;

/**
 * 会员视图对象 fit_member
 *
 * @author CodeBuddy
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = FitMember.class)
public class FitnessMemberVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会员ID
     */
    @ExcelProperty(value = "会员ID")
    private Long memberId;

    /**
     * 会员编号
     */
    @ExcelProperty(value = "会员编号")
    private String memberNo;

    /**
     * 真实姓名
     */
    @ExcelProperty(value = "真实姓名")
    private String realName;

    /**
     * 昵称
     */
    @ExcelProperty(value = "昵称")
    private String nickname;

    /**
     * 手机号
     */
    @ExcelProperty(value = "手机号")
    private String mobile;

    /**
     * 性别（0未知 1男 2女）
     */
    @ExcelProperty(value = "性别")
    private String gender;

    /**
     * 归属门店ID
     */
    private Long homeStoreId;

    /**
     * 归属门店名称
     */
    @ExcelProperty(value = "归属门店")
    private String homeStoreName;

    /**
     * 会员状态
     */
    @ExcelProperty(value = "会员状态")
    private String memberStatus;

    /**
     * 最近消费门店ID
     */
    private Long lastStoreId;

    /**
     * 最近消费门店名称
     */
    private String lastStoreName;

    /**
     * 最近到店时间
     */
    @ExcelProperty(value = "最近到店时间")
    private Date lastVisitTime;

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
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}
