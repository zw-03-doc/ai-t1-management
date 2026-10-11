export interface FitnessCheckinVO extends BaseEntity {
  checkinId: number | string;
  checkinNo?: string;
  reservationId?: number | string;

  scheduleId?: number | string;
  storeId?: number | string;
  storeName?: string;
  courseId?: number | string;
  courseName?: string;
  courseType?: string;
  coachId?: number | string;
  coachName?: string;
  memberId?: number | string;
  memberName?: string;
  memberMobile?: string;
  cardId?: number | string;
  reservationType?: string;
  cardName?: string;
  planType?: string;
  cardStatus?: string;

  remainTimes?: number;
  balanceAmount?: number;
  scheduleDate?: string;
  startTime?: string;
  endTime?: string;
  checkinType?: string;
  checkinTime?: string;
  consumeTimes?: number;
  consumeAmount?: number;
  operatorUserId?: number | string;
  operatorUserName?: string;
  checkinStatus?: string;
  remark?: string;
}

export interface FitnessCheckinQuery extends PageQuery {
  checkinNo?: string;
  keyword?: string;
  reservationId?: number | string;
  scheduleId?: number | string;
  storeId?: number | string;
  memberId?: number | string;
  coachId?: number | string;
  checkinType?: string;
  checkinStatus?: string;
}

export interface FitnessCheckinForm {
  checkinId?: number | string;
  checkinNo?: string;
  reservationId?: number | string;

  scheduleId?: number | string;
  storeId?: number | string;
  memberId?: number | string;
  cardId?: number | string;
  coachId?: number | string;
  checkinType?: string;
  checkinStatus?: string;
  consumeTimes?: number;
  consumeAmount?: number;
  remark?: string;
}
