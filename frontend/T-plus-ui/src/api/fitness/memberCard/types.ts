export interface FitnessMemberCardVO extends BaseEntity {
  cardId: number | string;
  storeId?: number | string;
  storeName?: string;
  memberId?: number | string;
  memberName?: string;
  memberMobile?: string;
  planId?: number | string;
  planName?: string;
  cardNo?: string;
  cardName?: string;
  planType?: string;
  bindCoachId?: number | string;
  coachName?: string;
  activeTime?: string;
  expireTime?: string;
  totalTimes?: number;
  usedTimes?: number;
  remainTimes?: number;
  balanceAmount?: number;
  cardStatus?: string;
  orderId?: number | string;
  orderNo?: string;
  remark?: string;
}

export interface FitnessMemberCardQuery extends PageQuery {
  keyword?: string;
  storeId?: number | string;
  memberId?: number | string;
  planId?: number | string;
  planType?: string;
  cardStatus?: string;
}

export interface FitnessMemberCardForm {
  cardId?: number | string;
  storeId?: number | string;
  memberId?: number | string;
  planId?: number | string;
  orderId?: number | string;
  bindCoachId?: number | string;
  cardNo?: string;
  cardName?: string;
  planType?: string;
  cardStatus?: string;
  totalTimesAdjust?: number;
  remainTimesAdjust?: number;
  balanceAmountAdjust?: number;
  expireTime?: string;
  remark?: string;
}
