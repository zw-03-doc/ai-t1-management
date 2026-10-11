export interface FitnessMemberVO extends BaseEntity {
  memberId: number | string;
  memberNo: string;
  realName?: string;
  nickname?: string;
  mobile?: string;
  gender?: string;
  homeStoreId?: number | string;
  homeStoreName?: string;
  memberStatus?: string;
  lastStoreId?: number | string;
  lastStoreName?: string;
  lastVisitTime?: string;
  birthday?: string;
  avatarUrl?: string;
  registerSource?: string;
  remark?: string;
}

export interface FitnessMemberQuery extends PageQuery {
  memberNo?: string;
  realName?: string;
  mobile?: string;
  memberStatus?: string;
}

import type { FitnessMemberCardVO } from '../memberCard/types';
import type { FitnessOrderVO } from '../order/types';
import type { FitnessReservationVO } from '../reservation/types';
import type { FitnessCheckinVO } from '../checkin/types';

export interface FitnessMemberDetailVO {
  member?: FitnessMemberVO;
  cardList?: FitnessMemberCardVO[];
  orderList?: FitnessOrderVO[];
  reservationList?: FitnessReservationVO[];
  checkinList?: FitnessCheckinVO[];
  cardCount?: number;
  activeCardCount?: number;
  orderCount?: number;
  reservationCount?: number;
  checkinCount?: number;
}

export interface FitnessMemberForm {
  memberId?: number | string;
  memberNo: string;
  homeStoreId?: number | string;
  mobile?: string;
  realName?: string;
  nickname?: string;
  gender?: string;
  birthday?: string;
  avatarUrl?: string;
  registerSource?: string;
  memberStatus?: string;
  remark?: string;
}
