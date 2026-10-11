export interface FitnessReservationVO extends BaseEntity {
  reservationId: number | string;
  reservationNo?: string;
  storeId?: number | string;

  storeName?: string;
  scheduleId?: number | string;
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
  scheduleDate?: string;

  startTime?: string;
  endTime?: string;
  reservationSource?: string;
  reservationStatus?: string;
  reservationTime?: string;
  cancelTime?: string;
  cancelReason?: string;
  consumeMode?: string;
  checkinId?: number | string;
  checkinTime?: string;
  checkinStatus?: string;
  remark?: string;
}

export interface FitnessReservationQuery extends PageQuery {
  reservationNo?: string;
  keyword?: string;
  storeId?: number | string;
  scheduleId?: number | string;
  memberId?: number | string;
  reservationSource?: string;
  reservationStatus?: string;
  consumeMode?: string;
}

export interface FitnessReservationForm {
  reservationId?: number | string;
  reservationNo?: string;
  storeId?: number | string;

  scheduleId?: number | string;
  memberId?: number | string;
  cardId?: number | string;
  reservationType?: string;
  courseId?: number | string;

  coachId?: number | string;
  reservationSource?: string;
  reservationStatus?: string;
  reservationTime?: string;
  cancelTime?: string;
  cancelReason?: string;
  consumeMode?: string;
  remark?: string;
}
