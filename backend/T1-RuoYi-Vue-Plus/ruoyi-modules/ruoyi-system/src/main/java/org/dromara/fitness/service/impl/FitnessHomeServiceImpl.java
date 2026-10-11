package org.dromara.fitness.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.fitness.domain.bo.FitnessHomeBo;
import org.dromara.fitness.domain.vo.FitnessHomeOverviewVo;
import org.dromara.fitness.domain.vo.FitnessHomeScheduleVo;
import org.dromara.fitness.domain.vo.FitnessHomeShortcutVo;
import org.dromara.fitness.domain.vo.FitnessHomeStatsVo;
import org.dromara.fitness.domain.vo.FitnessHomeTodoVo;
import org.dromara.fitness.mapper.FitnessHomeMapper;
import org.dromara.fitness.service.IFitnessHomeService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;


/**
 * 健身房 PC 首页服务实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessHomeServiceImpl implements IFitnessHomeService {

    private final FitnessHomeMapper baseMapper;

    @Override
    public FitnessHomeOverviewVo queryOverview(FitnessHomeBo bo) {
        FitnessHomeStatsVo stats = baseMapper.selectHomeStats(bo);
        if (stats == null) {
            stats = new FitnessHomeStatsVo();
        }
        fillDefaultValue(stats);

        List<FitnessHomeScheduleVo> scheduleList = baseMapper.selectTodayScheduleList(bo);

        FitnessHomeOverviewVo overviewVo = new FitnessHomeOverviewVo();
        overviewVo.setStats(stats);
        overviewVo.setTodoList(buildTodoList(stats));
        overviewVo.setShortcutList(buildShortcutList());
        overviewVo.setScheduleList(scheduleList == null ? Collections.emptyList() : scheduleList);
        return overviewVo;
    }


    private void fillDefaultValue(FitnessHomeStatsVo stats) {
        if (stats.getTodayArrivalCount() == null) {
            stats.setTodayArrivalCount(0L);
        }
        if (stats.getTodayNewMemberCount() == null) {
            stats.setTodayNewMemberCount(0L);
        }
        if (stats.getTodayReservationCount() == null) {
            stats.setTodayReservationCount(0L);
        }
        if (stats.getTodayCheckinCount() == null) {
            stats.setTodayCheckinCount(0L);
        }
        if (stats.getMonthNewMemberCount() == null) {
            stats.setMonthNewMemberCount(0L);
        }
        if (stats.getMonthRechargeOrderCount() == null) {
            stats.setMonthRechargeOrderCount(0L);
        }
        if (stats.getMonthReservationCount() == null) {
            stats.setMonthReservationCount(0L);
        }
        if (stats.getPendingCheckinCount() == null) {
            stats.setPendingCheckinCount(0L);
        }
        if (stats.getPendingReservationCount() == null) {
            stats.setPendingReservationCount(0L);
        }
        if (stats.getPendingOrderCount() == null) {
            stats.setPendingOrderCount(0L);
        }
        if (stats.getExpiringCardCount() == null) {
            stats.setExpiringCardCount(0L);
        }
        if (stats.getTodaySalesAmount() == null) {
            stats.setTodaySalesAmount(BigDecimal.ZERO);
        }
        if (stats.getMonthPrivateCourseSalesAmount() == null) {
            stats.setMonthPrivateCourseSalesAmount(BigDecimal.ZERO);
        }
    }

    private List<FitnessHomeTodoVo> buildTodoList(FitnessHomeStatsVo stats) {
        return List.of(
            buildTodo("pendingCheckin", "待签到课程", stats.getPendingCheckinCount(), "/fitness-course/checkin"),
            buildTodo("pendingReservation", "待处理预约", stats.getPendingReservationCount(), "/fitness-course/reservation"),
            buildTodo("pendingOrder", "待支付订单", stats.getPendingOrderCount(), "/fitness-order/order"),
            buildTodo("expiringCard", "即将到期卡项", stats.getExpiringCardCount(), "/fitness-member/memberCard")
        );
    }


    private FitnessHomeTodoVo buildTodo(String code, String name, Long count, String route) {
        FitnessHomeTodoVo todoVo = new FitnessHomeTodoVo();
        todoVo.setTodoCode(code);
        todoVo.setTodoName(name);
        todoVo.setTodoCount(count);
        todoVo.setRoute(route);
        return todoVo;
    }

    private List<FitnessHomeShortcutVo> buildShortcutList() {
        return List.of(
            buildShortcut("memberAdd", "新增会员", "/fitness-member/member", "fitness:member:add", "user"),
            buildShortcut("orderCreate", "录入订单", "/fitness-order/order", "fitness:order:add", "money"),
            buildShortcut("scheduleAdd", "快速排课", "/fitness-course/schedule", "fitness:schedule:add", "date"),
            buildShortcut("checkinHandle", "签到核销", "/fitness-course/checkin", "fitness:checkin:edit", "form")
        );
    }


    private FitnessHomeShortcutVo buildShortcut(String code, String name, String route, String permission, String icon) {
        FitnessHomeShortcutVo shortcutVo = new FitnessHomeShortcutVo();
        shortcutVo.setShortcutCode(code);
        shortcutVo.setShortcutName(name);
        shortcutVo.setRoute(route);
        shortcutVo.setPermission(permission);
        shortcutVo.setIcon(icon);
        return shortcutVo;
    }
}
