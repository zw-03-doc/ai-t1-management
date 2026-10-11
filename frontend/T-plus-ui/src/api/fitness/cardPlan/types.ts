export interface FitnessCardPlanVO extends BaseEntity {
  planId: number | string;
  planNo?: string;
  storeId?: number | string;

  storeName?: string;
  planName: string;
  planType: string;
  salePrice?: number;
  originalPrice?: number;
  validDays?: number;
  totalTimes?: number;
  giftTimes?: number;
  storedAmount?: number;
  supportCourseType?: string;
  isPrivateBindCoach?: string;
  customCoachId?: number | string;
  customCoachName?: string;
  customMemberIdsRaw?: string;
  customMemberIds?: Array<number | string>;
  customMemberNames?: string;
  bindCourseId?: number | string;
  bindCourseName?: string;
  sortNo?: number;
  status?: string;
  remark?: string;
}

export interface FitnessCardPlanQuery extends PageQuery {
  planNo?: string;
  planName?: string;
  planType?: string;
  storeId?: number | string;
  status?: string;
}

export interface FitnessCardPlanForm {
  planId?: number | string;
  planNo?: string;
  storeId?: number | string;

  planName: string;
  planType: string;
  salePrice?: number;
  originalPrice?: number;
  validDays?: number;
  totalTimes?: number;
  giftTimes?: number;
  storedAmount?: number;
  supportCourseType?: string;
  isPrivateBindCoach?: string;
  customCoachId?: number | string;
  customMemberIds?: Array<number | string>;
  bindCourseId?: number | string;
  sortNo?: number;
  status?: string;
  remark?: string;
}
