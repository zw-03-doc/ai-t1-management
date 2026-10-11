export interface FitnessReportQuery {
  storeId?: number | string;
  startDate?: string;
  endDate?: string;
}

export interface FitnessMemberReportVO {
  storeId?: number | string;
  storeName?: string;
  newMemberCount?: number;
  activeMemberCount?: number;
  paidMemberCount?: number;
  activeCardCount?: number;
  expiringCardCount?: number;
}

export interface FitnessOrderReportVO {
  storeId?: number | string;
  storeName?: string;
  orderType?: string;
  orderCount?: number;
  paidOrderCount?: number;
  refundOrderCount?: number;
  payableAmount?: number;
  paidAmount?: number;
  refundAmount?: number;
}

export interface FitnessCourseReportVO {
  storeId?: number | string;
  storeName?: string;
  courseId?: number | string;
  courseName?: string;
  courseType?: string;
  scheduleCount?: number;
  reservationCount?: number;
  checkinCount?: number;
  attendanceRate?: number;
}

export interface FitnessCoachStoreReportVO {
  storeId?: number | string;
  storeName?: string;
  coachId?: number | string;
  coachName?: string;
  scheduleCount?: number;
  reservationCount?: number;
  checkinCount?: number;
  privateCourseSalesAmount?: number;
}
