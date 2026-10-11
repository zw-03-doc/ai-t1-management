package org.dromara.fitness.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.fitness.domain.FitCheckinRecord;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 签到核销业务对象 fit_checkin_record
 *
 * @author CodeBuddy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = FitCheckinRecord.class, reverseConvertGenerate = false)
public class FitnessCheckinBo extends BaseEntity {

    private Long checkinId;

    private String checkinNo;

    private String reservationNo;

    private Long reservationId;



    private Long scheduleId;

    private Long storeId;

    private Long memberId;

    private Long cardId;

    private Long coachId;

    private String keyword;

    private String checkinType;

    private String checkinStatus;

    private Date checkinTimeStart;

    private Date checkinTimeEnd;

    private Date scheduleDateStart;

    private Date scheduleDateEnd;

    private Integer consumeTimes;

    private BigDecimal consumeAmount;

    private String remark;
}
