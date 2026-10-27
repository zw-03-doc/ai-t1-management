export interface FitnessCoachVO extends BaseEntity {
  coachId: number | string;
  coachNo?: string;
  activationCode?: string;
  userId?: number | string;

  userName?: string;

  nickName?: string;
  storeId?: number | string;
  storeName?: string;
  coachName: string;
  mobile?: string;
  contactWechat?: string;
  gender?: string;
  avatarUrl?: string;
  skillTags?: string;
  levelCode?: string;
  intro?: string;
  hireStatus?: string;
  remark?: string;
}

export interface FitnessCoachQuery extends PageQuery {
  coachNo?: string;
  coachName?: string;
  mobile?: string;
  contactWechat?: string;
  storeId?: number | string;
  hireStatus?: string;
}

export interface FitnessCoachForm {
  coachId?: number | string;
  coachNo?: string;
  userId?: number | string;

  storeId?: number | string;
  coachName: string;
  mobile?: string;
  contactWechat?: string;
  activationCode?: string;
  gender?: string;

  avatarUrl?: string;
  skillTags?: string;
  levelCode?: string;
  intro?: string;
  hireStatus?: string;
  remark?: string;
}
