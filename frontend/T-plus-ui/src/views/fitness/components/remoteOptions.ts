import { getCardPlan, listCardPlan } from '@/api/fitness/cardPlan';
import { getCoach, listCoach } from '@/api/fitness/coach';
import { getCourse, listCourse } from '@/api/fitness/course';
import { getMember, listMember } from '@/api/fitness/member';
import { getMemberCard, listMemberCard } from '@/api/fitness/memberCard';
import { getSchedule, listSchedule } from '@/api/fitness/schedule';
import { getStore, listStore } from '@/api/fitness/store';
import { getUser, listUser } from '@/api/system/user';

export interface RemoteSelectOption {
  label: string;
  value: string | number | boolean;
}

export interface RemoteSelectParams {
  keyword?: string;
  value?: string | number | boolean | Array<string | number | boolean> | null | undefined;
  model?: Record<string, any>;
}


const DEFAULT_PAGE_NUM = 1;
const DEFAULT_PAGE_SIZE = 20;

const hasValue = (value: unknown) => value !== undefined && value !== null && value !== '';

const normalizeRows = <T = any>(res: any): T[] => {
  if (Array.isArray(res?.rows)) {
    return res.rows;
  }
  if (Array.isArray(res?.data?.rows)) {
    return res.data.rows;
  }
  if (Array.isArray(res?.data)) {
    return res.data;
  }
  return [];
};

const uniqueOptions = (items: RemoteSelectOption[]) => {
  const map = new Map<string, RemoteSelectOption>();
  items.forEach((item) => {
    if (!item || !hasValue(item.value)) {
      return;
    }
    map.set(String(item.value), item);
  });
  return Array.from(map.values());
};

const filterOptions = (items: RemoteSelectOption[], keyword?: string) => {
  const text = keyword?.trim().toLowerCase();
  if (!text) {
    return items;
  }
  return items.filter((item) => item.label.toLowerCase().includes(text) || String(item.value).toLowerCase().includes(text));
};

async function buildOptions<T = any>(params: {
  keyword?: string;
  value?: string | number | boolean | Array<string | number | boolean> | null | undefined;
  listRequest: () => Promise<any>;
  getRequest?: (value: string | number | boolean) => Promise<any>;
  getData?: (res: any) => T | undefined;
  toOption: (item: T) => RemoteSelectOption;
}) {

  const { keyword, value, listRequest, getRequest, getData, toOption } = params;
  const tasks: Promise<RemoteSelectOption[]>[] = [
    listRequest()
      .then((res) => normalizeRows<T>(res).map((item) => toOption(item)))
      .catch(() => [])
  ];

  if (hasValue(value) && getRequest) {
    const values = Array.isArray(value) ? value : [value as string | number | boolean];
    values.forEach((targetValue) => {
      if (!hasValue(targetValue)) {
        return;
      }
      tasks.push(
        getRequest(targetValue as string | number | boolean)
          .then((res) => {
            const data = getData ? getData(res) : res?.data;
            return data ? [toOption(data)] : [];
          })
          .catch(() => [])
      );
    });
  }


  const result = (await Promise.all(tasks)).flat();
  return filterOptions(uniqueOptions(result), keyword);
}

export async function loadStoreOptions({ keyword, value }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    value,
    listRequest: () => listStore({ pageNum: DEFAULT_PAGE_NUM, pageSize: DEFAULT_PAGE_SIZE, storeName: keyword || undefined }),
    getRequest: (target) => getStore(target as string | number),
    toOption: (item: any) => ({
      value: item.storeId,
      label: `${item.storeName || '未命名门店'}${item.storeCode ? `（${item.storeCode}）` : ''}`
    })
  });
}

export async function loadCoachOptions({ keyword, value }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    value,
    listRequest: () =>
      listCoach({
        pageNum: DEFAULT_PAGE_NUM,
        pageSize: DEFAULT_PAGE_SIZE,
        coachNo: keyword || undefined,
        coachName: keyword || undefined,
        mobile: keyword || undefined
      }),
    getRequest: (target) => getCoach(target as string | number),
    toOption: (item: any) => ({
      value: item.coachId,
      label: `${item.coachName || '未命名教练'}${item.coachNo ? `｜${item.coachNo}` : ''}${item.mobile ? `（${item.mobile}）` : ''}`
    })
  });
}


export async function loadMemberOptions({ keyword, value }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    value,
    listRequest: () =>
      listMember({
        pageNum: DEFAULT_PAGE_NUM,
        pageSize: DEFAULT_PAGE_SIZE,
        realName: keyword || undefined,
        mobile: keyword || undefined,
        memberNo: keyword || undefined
      }),
    getRequest: (target) => getMember(target as string | number),
    toOption: (item: any) => ({
      value: item.memberId,
      label: `${item.realName || item.nickname || '未命名会员'}${item.mobile ? `（${item.mobile}）` : ''}${item.memberNo ? `｜${item.memberNo}` : ''}`
    })
  });
}

export async function loadCourseOptions({ keyword, value }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    value,
    listRequest: () =>
      listCourse({
        pageNum: DEFAULT_PAGE_NUM,
        pageSize: DEFAULT_PAGE_SIZE,
        courseNo: keyword || undefined,
        courseName: keyword || undefined
      }),
    getRequest: (target) => getCourse(target as string | number),
    toOption: (item: any) => ({
      value: item.courseId,
      label: `${item.courseName || '未命名课程'}${item.courseNo ? `｜${item.courseNo}` : ''}${item.courseType ? `（${item.courseType}）` : ''}`
    })
  });
}


export async function loadCardPlanOptions({ keyword, value }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    value,
    listRequest: () =>
      listCardPlan({
        pageNum: DEFAULT_PAGE_NUM,
        pageSize: DEFAULT_PAGE_SIZE,
        planNo: keyword || undefined,
        planName: keyword || undefined
      }),
    getRequest: (target) => getCardPlan(target as string | number),
    toOption: (item: any) => ({
      value: item.planId,
      label: `${item.planName || '未命名方案'}${item.planNo ? `｜${item.planNo}` : ''}${item.planType ? `（${item.planType}）` : ''}`
    })
  });
}


export async function loadScheduleOptions({ keyword, value, model }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    value,
    listRequest: () =>
      listSchedule({
        pageNum: DEFAULT_PAGE_NUM,
        pageSize: DEFAULT_PAGE_SIZE,
        scheduleNo: keyword || undefined,
        storeId: model?.storeId || undefined,
        courseId: model?.courseId || undefined,
        coachId: model?.coachId || undefined
      }),
    getRequest: (target) => getSchedule(target as string | number),
    toOption: (item: any) => ({
      value: item.scheduleId,
      label: `${item.scheduleNo || item.scheduleId}｜${item.scheduleDate || ''} ${item.startTime?.slice?.(11, 16) || ''}-${item.endTime?.slice?.(11, 16) || ''} ${item.courseName || ''} / ${item.coachName || ''}`.trim()
    })
  });
}


export async function loadMemberCardOptions({ keyword, value }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    value,
    listRequest: () => listMemberCard({ pageNum: DEFAULT_PAGE_NUM, pageSize: DEFAULT_PAGE_SIZE, keyword: keyword || undefined }),
    getRequest: (target) => getMemberCard(target as string | number),
    toOption: (item: any) => ({
      value: item.cardId,
      label: `${item.cardName || item.planName || '会员卡'}${item.memberName ? `｜${item.memberName}` : ''}${item.cardNo ? `｜${item.cardNo}` : ''}`
    })
  });
}

export async function loadSystemUserOptions({ keyword, value }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    value,
    listRequest: () =>
      listUser({
        pageNum: DEFAULT_PAGE_NUM,
        pageSize: DEFAULT_PAGE_SIZE,
        userName: keyword || undefined,
        nickName: keyword || undefined,
        phonenumber: keyword || undefined
      }),
    getRequest: (target) => getUser(target as string | number),
    getData: (res) => res?.data?.user,
    toOption: (item: any) => ({
      value: item.userId,
      label: `${item.nickName || item.userName || '未命名用户'}${item.userName ? `（${item.userName}）` : ''}`
    })
  });
}
