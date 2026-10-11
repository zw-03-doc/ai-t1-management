package org.dromara.fitness.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.fitness.domain.FitCourseReservation;

import java.util.Date;

/**
 * 预约业务对象 fit_course_reservation
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = FitCourseReservation.class, reverseConvertGenerate = false)
public class FitnessReservationBo extends BaseEntity {

    @NotNull(message = "预约ID不能为空", groups = { EditGroup.class })
    private Long reservationId;

    private String reservationNo;

    private Long storeId;


    @NotNull(message = "排课ID不能为空", groups = { AddGroup.class })
    private Long scheduleId;

    private String scheduleNo;

    @NotNull(message = "会员ID不能为空", groups = { AddGroup.class })

    private Long memberId;

    private Long cardId;

    /**
     * 预约类型（store_experience/card）
     */
    private String reservationType;

    private Long courseId;


    private Long coachId;

    private String keyword;

    private String reservationSource;

    private String reservationStatus;

    private Date reservationTime;

    private Date cancelTime;

    private String cancelReason;

    private String consumeMode;

    private Date reservationTimeStart;

    private Date reservationTimeEnd;

    private Date scheduleDateStart;

    private Date scheduleDateEnd;

    private String remark;
}
