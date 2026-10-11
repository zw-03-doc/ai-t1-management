export interface FitnessHomeStatsVO {
  todayArrivalCount?: number;
  todayNewMemberCount?: number;
  todaySalesAmount?: number;
  todayReservationCount?: number;
  todayCheckinCount?: number;
  monthNewMemberCount?: number;
  monthRechargeOrderCount?: number;
  monthReservationCount?: number;
  monthPrivateCourseSalesAmount?: number;
  pendingCheckinCount?: number;
  pendingReservationCount?: number;
  pendingOrderCount?: number;
  expiringCardCount?: number;
}

export interface FitnessHomeTodoVO {
  todoCode?: string;
  todoName?: string;
  todoCount?: number;
  route?: string;
}

export interface FitnessHomeShortcutVO {
  shortcutCode?: string;
  shortcutName?: string;
  route?: string;
  permission?: string;
  icon?: string;
}

export interface FitnessHomeScheduleVO {
  scheduleId?: number;
  scheduleNo?: string;
  courseName?: string;
  storeName?: string;
  coachName?: string;
  startTime?: string;
  endTime?: string;
  capacity?: number;
  reservedCount?: number;
  signedCount?: number;
  scheduleStatus?: string;
}

export interface FitnessHomeOverviewVO {
  stats?: FitnessHomeStatsVO;
  todoList?: FitnessHomeTodoVO[];
  shortcutList?: FitnessHomeShortcutVO[];
  scheduleList?: FitnessHomeScheduleVO[];
}

