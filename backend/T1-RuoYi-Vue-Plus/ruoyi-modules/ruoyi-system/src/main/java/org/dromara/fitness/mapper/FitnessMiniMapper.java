package org.dromara.fitness.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.fitness.domain.bo.FitnessMiniBo;
import org.dromara.fitness.domain.vo.FitnessMiniVo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 会员小程序 Mapper
 *
 * @author CodeBuddy
 */
public interface FitnessMiniMapper {

    FitnessMiniVo.MemberInfoVo selectMemberByOpenid(@Param("openid") String openid);

    FitnessMiniVo.MemberInfoVo selectMemberByMobile(@Param("mobile") String mobile);

    FitnessMiniVo.MemberInfoVo selectMemberById(@Param("memberId") Long memberId);

    Long countMemberByMobile(@Param("tenantId") String tenantId,
                             @Param("mobile") String mobile,
                             @Param("excludeMemberId") Long excludeMemberId);

    int insertMember(@Param("memberId") Long memberId,
                     @Param("memberNo") String memberNo,
                     @Param("tenantId") String tenantId,
                     @Param("bo") FitnessMiniBo.WechatLoginBo bo);

    int updateMemberLoginProfile(@Param("memberId") Long memberId,
                                 @Param("bo") FitnessMiniBo.WechatLoginBo bo);

    int updateMemberPhone(@Param("memberId") Long memberId,
                          @Param("bo") FitnessMiniBo.BindPhoneBo bo);

    int updateMemberProfile(@Param("memberId") Long memberId,
                            @Param("bo") FitnessMiniBo.ProfileUpdateBo bo);

    FitnessMiniVo.MineSummaryVo selectMineSummary(@Param("memberId") Long memberId);

    FitnessMiniVo.ProfileVo selectMemberProfile(@Param("memberId") Long memberId);

    List<FitnessMiniVo.StoreVo> selectStoreList(@Param("keyword") String keyword,
                                                @Param("memberId") Long memberId);

    FitnessMiniVo.StoreVo selectStoreById(@Param("storeId") Long storeId,
                                          @Param("memberId") Long memberId);

    List<FitnessMiniVo.CourseVo> selectCourseList(@Param("bo") FitnessMiniBo.CourseQueryBo bo);

    FitnessMiniVo.CourseVo selectCourseById(@Param("courseId") Long courseId);

    List<FitnessMiniVo.CourseVo> selectRecommendCourseList(@Param("memberId") Long memberId);

    List<FitnessMiniVo.ScheduleVo> selectScheduleList(@Param("memberId") Long memberId,
                                                      @Param("bo") FitnessMiniBo.ScheduleQueryBo bo);

    FitnessMiniVo.ReservationActionVo selectReservationAction(@Param("memberId") Long memberId,
                                                              @Param("reservationId") Long reservationId);

    FitnessMiniVo.ReservationActionVo selectScheduleReservationAction(@Param("memberId") Long memberId,
                                                                      @Param("scheduleId") Long scheduleId);

    Long countActiveReservation(@Param("memberId") Long memberId,
                                @Param("scheduleId") Long scheduleId);

    FitnessMiniVo.MemberCardVo selectUsableMemberCard(@Param("memberId") Long memberId,
                                                      @Param("cardId") Long cardId);

    int insertReservation(@Param("reservationId") Long reservationId,
                          @Param("reservationNo") String reservationNo,
                          @Param("tenantId") String tenantId,
                          @Param("memberId") Long memberId,
                          @Param("scheduleId") Long scheduleId,
                          @Param("storeId") Long storeId,
                          @Param("cardId") Long cardId,
                          @Param("reservationType") String reservationType,
                          @Param("consumeMode") String consumeMode,
                          @Param("remark") String remark,
                          @Param("reservationTime") Date reservationTime);


    int updateReservationCancel(@Param("reservationId") Long reservationId,
                                @Param("cancelReason") String cancelReason,
                                @Param("cancelTime") Date cancelTime);

    int updateScheduleReservedCount(@Param("scheduleId") Long scheduleId,
                                    @Param("delta") Integer delta);

    List<FitnessMiniVo.ReservationVo> selectMyReservationList(@Param("memberId") Long memberId);

    FitnessMiniVo.ReservationVo selectReservationById(@Param("memberId") Long memberId,
                                                      @Param("reservationId") Long reservationId);

    List<FitnessMiniVo.CardPlanVo> selectCardPlanList(@Param("storeId") Long storeId);

    FitnessMiniVo.CardPlanVo selectCardPlanById(@Param("planId") Long planId);

    List<FitnessMiniVo.ActivityVo> selectActivityList(@Param("storeId") Long storeId);

    FitnessMiniVo.ActivityVo selectActivityById(@Param("activityId") Long activityId);

    List<FitnessMiniVo.ActivityCardPlanVo> selectActivityCardPlanList(@Param("activityId") Long activityId);

    int insertOrder(@Param("orderId") Long orderId,

                    @Param("tenantId") String tenantId,
                    @Param("memberId") Long memberId,
                    @Param("storeId") Long storeId,
                    @Param("orderNo") String orderNo,
                    @Param("orderType") String orderType,
                    @Param("orderStatus") String orderStatus,
                    @Param("payMethod") String payMethod,
                    @Param("totalAmount") BigDecimal totalAmount,
                    @Param("discountAmount") BigDecimal discountAmount,
                    @Param("payableAmount") BigDecimal payableAmount,
                    @Param("paidAmount") BigDecimal paidAmount,
                    @Param("paidTime") Date paidTime,
                    @Param("remark") String remark);

    int insertOrderItem(@Param("itemId") Long itemId,
                        @Param("itemNo") String itemNo,
                        @Param("tenantId") String tenantId,
                        @Param("orderId") Long orderId,
                        @Param("itemType") String itemType,
                        @Param("itemRefId") Long itemRefId,
                        @Param("itemName") String itemName,
                        @Param("quantity") Integer quantity,
                        @Param("unitPrice") BigDecimal unitPrice,
                        @Param("totalPrice") BigDecimal totalPrice,
                        @Param("discountAmount") BigDecimal discountAmount,
                        @Param("realAmount") BigDecimal realAmount,
                        @Param("grantCardId") Long grantCardId);

    int insertMemberCard(@Param("cardId") Long cardId,
                         @Param("tenantId") String tenantId,
                         @Param("storeId") Long storeId,
                         @Param("memberId") Long memberId,
                         @Param("plan") FitnessMiniVo.CardPlanVo plan,
                         @Param("cardNo") String cardNo,
                         @Param("bindCoachId") Long bindCoachId,
                         @Param("activeTime") Date activeTime,
                         @Param("expireTime") Date expireTime,
                         @Param("totalTimes") Integer totalTimes,
                         @Param("balanceAmount") BigDecimal balanceAmount,
                         @Param("cardStatus") String cardStatus,
                         @Param("orderId") Long orderId);

    List<FitnessMiniVo.OrderVo> selectMyOrderList(@Param("memberId") Long memberId);

    FitnessMiniVo.OrderVo selectOrderById(@Param("memberId") Long memberId,
                                          @Param("orderId") Long orderId);

    List<FitnessMiniVo.OrderItemVo> selectOrderItemList(@Param("orderId") Long orderId);

    List<FitnessMiniVo.MemberCardVo> selectMyMemberCardList(@Param("memberId") Long memberId);

    FitnessMiniVo.CheckinVo selectCheckinByReservationId(@Param("memberId") Long memberId,
                                                         @Param("reservationId") Long reservationId);

    int insertCheckin(@Param("checkinId") Long checkinId,
                      @Param("checkinNo") String checkinNo,
                      @Param("tenantId") String tenantId,

                      @Param("storeId") Long storeId,
                      @Param("reservationId") Long reservationId,
                      @Param("scheduleId") Long scheduleId,
                      @Param("memberId") Long memberId,
                      @Param("cardId") Long cardId,
                      @Param("checkinType") String checkinType,
                      @Param("checkinTime") Date checkinTime,
                      @Param("remark") String remark);

    int updateMemberVisitInfo(@Param("memberId") Long memberId,
                              @Param("storeId") Long storeId,
                              @Param("visitTime") Date visitTime);

    List<FitnessMiniVo.CheckinVo> selectMyCheckinList(@Param("memberId") Long memberId);
}
