export interface FitnessCourseVO extends BaseEntity {
  courseId: number | string;
  courseNo?: string;
  courseName: string;

  courseType?: string;
  storeId?: number | string;
  storeName?: string;
  defaultCoachId?: number | string;
  coachName?: string;
  durationMinutes?: number;
  capacity?: number;
  coverUrl?: string;
  description?: string;
  supportCardType?: string;
  status?: string;
  remark?: string;
}

export interface FitnessCourseQuery extends PageQuery {
  courseNo?: string;
  courseName?: string;
  courseType?: string;
  storeId?: number | string;
  status?: string;
}

export interface FitnessCourseForm {
  courseId?: number | string;
  courseNo?: string;
  courseName: string;

  courseType?: string;
  storeId?: number | string;
  defaultCoachId?: number | string;
  durationMinutes?: number;
  capacity?: number;
  coverUrl?: string;
  description?: string;
  supportCardType?: string;
  status?: string;
  remark?: string;
}
