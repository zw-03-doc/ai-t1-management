package org.dromara.fitness.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 签到核销实体对象 fit_checkin_record
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fit_checkin_record")
public class FitCheckinRecord extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "checkin_id")
    private Long checkinId;

    private String checkinNo;

    private Long storeId;


    private Long reservationId;

    private Long scheduleId;

    private Long memberId;

    private Long cardId;

    private String checkinType;

    private Date checkinTime;

    private Integer consumeTimes;

    private BigDecimal consumeAmount;

    private Long operatorUserId;

    private String checkinStatus;

    private String remark;

    @Version
    private Integer version;

    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
