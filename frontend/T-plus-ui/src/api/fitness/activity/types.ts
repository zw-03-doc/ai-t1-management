export interface FitnessActivityVO extends BaseEntity {
  activityId: number | string;
  activityNo?: string;
  storeId?: number | string;
  storeName?: string;
  title: string;
  subTitle?: string;
  activityType?: string;
  actionType?: string;
  planIds?: Array<number | string>;
  discountLabel?: string;
  ruleNote?: string;
  priceHighlight?: string;
  targetAudience?: string;
  coverUrl?: string;
  detailImageUrl?: string;
  detailContent?: string;
  startTime?: string;
  endTime?: string;
  countdownEndTime?: string;
  weeklyFeatured?: string;
  sortNo?: number;
  status?: string;
  remark?: string;
}

export interface FitnessActivityQuery extends PageQuery {
  activityNo?: string;
  title?: string;
  storeId?: number | string;
  activityType?: string;
  actionType?: string;
  weeklyFeatured?: string;
  status?: string;
}

export interface FitnessActivityForm {
  activityId?: number | string;
  activityNo?: string;
  storeId?: number | string;
  title: string;
  subTitle?: string;
  activityType?: string;
  actionType?: string;
  planIds?: Array<number | string>;
  discountLabel?: string;
  ruleNote?: string;
  priceHighlight?: string;
  targetAudience?: string;
  coverUrl?: string;
  detailImageUrl?: string;
  detailContent?: string;
  startTime?: string;
  endTime?: string;
  countdownEndTime?: string;
  weeklyFeatured?: string;
  sortNo?: number;
  status?: string;
  remark?: string;
}
