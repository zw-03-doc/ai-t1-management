package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessMiniBo;
import org.dromara.fitness.domain.vo.FitnessMiniVo;

import java.util.List;

/**
 * 会员小程序服务接口
 *
 * @author CodeBuddy
 */
public interface IFitnessMiniService {

    FitnessMiniVo.LoginVo wechatLogin(FitnessMiniBo.WechatLoginBo bo);

    void bindPhone(FitnessMiniBo.BindPhoneBo bo);

    FitnessMiniVo.HomeIndexVo queryHomeIndex();

    List<FitnessMiniVo.StoreVo> queryStoreList(String keyword);

    FitnessMiniVo.StoreVo queryStoreById(Long storeId);

    List<FitnessMiniVo.CourseVo> queryCourseList(FitnessMiniBo.CourseQueryBo bo);

    FitnessMiniVo.CourseVo queryCourseById(Long courseId);

    TableDataInfo<FitnessMiniVo.ScheduleVo> queryScheduleList(FitnessMiniBo.ScheduleQueryBo bo, PageQuery pageQuery);

    FitnessMiniVo.ReservationVo createReservation(FitnessMiniBo.ReservationCreateBo bo);

    TableDataInfo<FitnessMiniVo.ReservationVo> queryMyReservationList(PageQuery pageQuery);

    void cancelReservation(Long reservationId);

    List<FitnessMiniVo.CardPlanVo> queryCardPlanList(Long storeId);

    FitnessMiniVo.CardPlanVo queryCardPlanById(Long planId);

    List<FitnessMiniVo.ActivityVo> queryActivityList(Long storeId);

    FitnessMiniVo.ActivityVo queryActivityById(Long activityId);

    List<FitnessMiniVo.ActivityCardPlanVo> queryActivityCardPlanList(Long activityId);

    FitnessMiniVo.OrderVo createOrder(FitnessMiniBo.OrderCreateBo bo);


    TableDataInfo<FitnessMiniVo.OrderVo> queryMyOrderList(PageQuery pageQuery);

    FitnessMiniVo.OrderVo queryOrderById(Long orderId);

    List<FitnessMiniVo.MemberCardVo> queryMyMemberCards();

    FitnessMiniVo.CheckinVo createCheckin(FitnessMiniBo.CheckinCreateBo bo);

    FitnessMiniVo.CheckinVo queryCheckinStatus(Long reservationId);

    TableDataInfo<FitnessMiniVo.CheckinVo> queryMyCheckinList(PageQuery pageQuery);

    FitnessMiniVo.MineSummaryVo queryMineSummary();

    FitnessMiniVo.ProfileVo queryProfile();

    void updateProfile(FitnessMiniBo.ProfileUpdateBo bo);
}
