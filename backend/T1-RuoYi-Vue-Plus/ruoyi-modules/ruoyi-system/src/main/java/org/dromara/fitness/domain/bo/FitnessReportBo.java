package org.dromara.fitness.domain.bo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 报表中心查询对象
 *
 * @author CodeBuddy
 */
@Data
public class FitnessReportBo {

    /**
     * 门店ID，为空时统计全部门店
     */
    private Long storeId;

    /**
     * 统计开始日期（含）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /**
     * 统计结束日期（含）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endDate;
}
