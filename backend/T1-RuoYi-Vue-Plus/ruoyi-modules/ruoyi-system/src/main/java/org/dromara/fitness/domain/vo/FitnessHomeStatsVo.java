package org.dromara.fitness.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 健身房 PC 首页统计视图对象
 *
 * @author CodeBuddy
 */
@Data
public class FitnessHomeStatsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 今日到店人数（按签到会员去重） */
    private Long todayArrivalCount;

    /** 今日新增会员数 */
    private Long todayNewMemberCount;

    /** 今日销售额 */
    private BigDecimal todaySalesAmount;

    /** 今日预约数（按今日排课统计） */
    private Long todayReservationCount;

    /** 今日签到数 */
    private Long todayCheckinCount;

    /** 本月会员增长 */
    private Long monthNewMemberCount;

    /** 本月续费单量 */
    private Long monthRechargeOrderCount;

    /** 本月课程预约数 */
    private Long monthReservationCount;

    /** 本月私教销售额 */
    private BigDecimal monthPrivateCourseSalesAmount;

    /** 待签到课程数 */
    private Long pendingCheckinCount;

    /** 待处理预约数 */
    private Long pendingReservationCount;

    /** 待支付订单数 */
    private Long pendingOrderCount;

    /** 即将到期卡项数（7天内） */
    private Long expiringCardCount;
}
