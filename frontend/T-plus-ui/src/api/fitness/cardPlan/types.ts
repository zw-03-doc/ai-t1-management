export interface FitnessCardPlanVO extends BaseEntity {
  grantedTimes?: number;
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
  allowedCourseIds?: Array<number | string>;
  bindCourseId?: number | string;
  allowedCourseNames?: string;
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
  storeId?: number | string | null;

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
  allowedCourseIds?: Array<number | string>;
  bindCourseId?: number | string | null;
  sortNo?: number;
  status?: string;
  remark?: string;
}
