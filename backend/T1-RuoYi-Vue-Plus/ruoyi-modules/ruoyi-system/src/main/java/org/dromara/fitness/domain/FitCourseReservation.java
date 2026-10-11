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
 * 课程预约实体对象 fit_course_reservation
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fit_course_reservation")
public class FitCourseReservation extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "reservation_id")
    private Long reservationId;

    private String reservationNo;

    private Long storeId;


    private Long scheduleId;

    private Long memberId;

    private Long cardId;

    /**
     * 预约类型（store_experience/card）
     */
    private String reservationType;

    private String reservationSource;


    private String reservationStatus;

    private Date reservationTime;

    private Date cancelTime;

    private String cancelReason;

    private String consumeMode;

    private String remark;

    @Version
    private Integer version;

    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
