package org.dromara.fitness.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.fitness.domain.FitStore;

/**
 * 门店业务对象 fit_store
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = FitStore.class, reverseConvertGenerate = false)
public class FitnessStoreBo extends BaseEntity {

    /**
     * 门店ID
     */
    @NotNull(message = "门店ID不能为空", groups = { EditGroup.class })
    private Long storeId;

    /**
     * 门店编码
     */
    @NotBlank(message = "门店编码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String storeCode;

    /**
     * 门店名称
     */
    @NotBlank(message = "门店名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String storeName;

    /**
     * 门店简称
     */
    private String storeShortName;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 省编码
     */
    private String provinceCode;

    /**
     * 市编码
     */
    private String cityCode;

    /**
     * 区县编码
     */
    private String districtCode;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 营业时间
     */
    private String businessHours;

    /**
     * 门店封面图
     */
    private String coverUrl;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
