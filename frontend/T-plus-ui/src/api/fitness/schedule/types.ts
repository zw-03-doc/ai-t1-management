export interface FitnessScheduleVO extends BaseEntity {
  scheduleId: number | string;
  scheduleNo?: string;
  storeId?: number | string;
  storeName?: string;
  courseId?: number | string;
  courseName?: string;
  courseType?: string;
  coachId?: number | string;
  coachName?: string;
  scheduleDate?: string;
  startTime?: string;
  endTime?: string;
  capacity?: number;
  reservedCount?: number;
  signedCount?: number;
  reservationDeadline?: string;
  cancelDeadline?: string;
  scheduleStatus?: string;
  scheduleMode?: string;
  studentMemberIds?: Array<number | string>;
  studentNames?: string;
  cancelReason?: string;
  remark?: string;
}

export interface FitnessScheduleQuery extends PageQuery {
  scheduleId?: number | string;
  scheduleNo?: string;
  storeId?: number | string;
  courseId?: number | string;
  coachId?: number | string;
  scheduleStatus?: string;
  scheduleMode?: string;
  scheduleDateStart?: string;
  scheduleDateEnd?: string;
}

export interface FitnessScheduleForm {
  scheduleId?: number | string;
  scheduleNo?: string;
  storeId?: number | string;
  courseId?: number | string;
  coachId?: number | string;
  scheduleDate?: string;
  startTime?: string;
  endTime?: string;
  capacity?: number;
  reservedCount?: number;
  signedCount?: number;
  reservationDeadline?: string;
  cancelDeadline?: string;
  scheduleStatus?: string;
  scheduleMode?: string;
  studentMemberIds?: Array<number | string>;
  cancelReason?: string;
  remark?: string;
}
