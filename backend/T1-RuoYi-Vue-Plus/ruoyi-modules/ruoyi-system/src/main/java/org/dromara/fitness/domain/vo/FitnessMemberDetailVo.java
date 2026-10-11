package org.dromara.fitness.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 会员详情聚合视图对象
 *
 * @author CodeBuddy
 */
@Data
public class FitnessMemberDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会员基本信息 */
    private FitnessMemberVo member;

    /** 持卡权益 */
    private List<FitnessMemberCardVo> cardList;

    /** 最近订单 */
    private List<FitnessOrderVo> orderList;

    /** 最近预约 */
    private List<FitnessReservationVo> reservationList;

    /** 最近签到核销 */
    private List<FitnessCheckinVo> checkinList;

    /** 持卡总数 */
    private Integer cardCount;

    /** 有效卡数 */
    private Integer activeCardCount;

    /** 订单总数 */
    private Integer orderCount;

    /** 预约总数 */
    private Integer reservationCount;

    /** 签到总数 */
    private Integer checkinCount;

}
