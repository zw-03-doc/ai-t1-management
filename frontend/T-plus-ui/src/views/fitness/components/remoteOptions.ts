import { getCardPlan, listCardPlan } from '@/api/fitness/cardPlan';
import { getCoach, listCoach } from '@/api/fitness/coach';
import { getCourse, listCourse } from '@/api/fitness/course';
import { getMember, listMember } from '@/api/fitness/member';
import { getMemberCard, listMemberCard } from '@/api/fitness/memberCard';
import { getSchedule, listSchedule } from '@/api/fitness/schedule';
import { getStore, listStore } from '@/api/fitness/store';
import { getUser, listUser } from '@/api/system/user';

export interface RemoteSelectOption {
  salePrice?: number;
  label: string;
  value: string | number | boolean;
}

export interface RemoteSelectParams {
  keyword?: string;
  pageNum?: number;
  value?: string | number | boolean | Array<string | number | boolean> | null | undefined;
  model?: Record<string, any>;
}


const DEFAULT_PAGE_SIZE = 20;
const courseTypeNames: Record<string, string> = { private: '私教课', small_group: '小班课', group: '团课' };
const planTypeNames: Record<string, string> = { private_card: '私教课包', group_card: '团课课包', times_card: '次卡', time_card: '期限卡', stored_card: '储值卡' };

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

async function buildOptions<T = any>(params: {
  keyword?: string;
  pageNum?: number;
  value?: string | number | boolean | Array<string | number | boolean> | null | undefined;
  listRequest: () => Promise<any>;
  getRequest?: (value: string | number | boolean) => Promise<any>;
  getData?: (res: any) => T | undefined;
  toOption: (item: T) => RemoteSelectOption;
}) {

  const { value, pageNum = 1, listRequest, getRequest, getData, toOption } = params;
  let hasMore = false;
  const tasks: Promise<RemoteSelectOption[]>[] = [
    listRequest()
      .then((res) => {
        const rows = normalizeRows<T>(res);
        const total = res?.total ?? res?.data?.total;
        hasMore = total != null ? pageNum * DEFAULT_PAGE_SIZE < Number(total) : rows.length === DEFAULT_PAGE_SIZE;
        return rows.map((item) => toOption(item));
      })
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
  return Object.assign(uniqueOptions(result), { hasMore });
}

export async function loadStoreOptions({ keyword, value, model, pageNum = 1 }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    pageNum,
    value,
    listRequest: () => listStore({ pageNum, pageSize: DEFAULT_PAGE_SIZE, storeName: keyword || undefined, status: model?.storeStatus }),
    getRequest: (target) => getStore(target as string | number),
    toOption: (item: any) => ({
      value: item.storeId,
      label: `${item.storeName || '未命名门店'}${item.storeCode ? `（${item.storeCode}）` : ''}`
    })
  });
}

export async function loadCoachOptions({ keyword, value, model, pageNum = 1 }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    pageNum,
    value,
    listRequest: () =>
      listCoach({
        pageNum,
        pageSize: DEFAULT_PAGE_SIZE,
        storeId: model?.storeId || undefined,
        hireStatus: model?.hireStatus || undefined,
        keyword: keyword?.trim() || undefined
      }),
    getRequest: (target) => getCoach(target as string | number),
    toOption: (item: any) => ({
      value: item.coachId,
      label: `${item.coachName || '未命名教练'}${item.coachNo ? `｜${item.coachNo}` : ''}${item.mobile ? `（${item.mobile}）` : ''}`
    })
  });
}


export async function loadMemberOptions({ keyword, value, model, pageNum = 1 }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    pageNum,
    value,
    listRequest: () =>
      listMember({
        pageNum,
        memberStatus: model?.memberStatus,
        pageSize: DEFAULT_PAGE_SIZE,
        keyword: keyword?.trim() || undefined
      }),
    getRequest: (target) => getMember(target as string | number),
    toOption: (item: any) => ({
      value: item.memberId,
      label: `${item.realName || item.nickname || '未命名会员'}${item.mobile ? `（${item.mobile}）` : ''}${item.memberNo ? `｜${item.memberNo}` : ''}`
    })
  });
}

export async function loadCourseOptions({ keyword, value, model, pageNum = 1 }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    pageNum,
    value,
    listRequest: () =>
      listCourse({
        pageNum,
        courseType: model?.courseTypeFilter,
        pageSize: DEFAULT_PAGE_SIZE,
        storeId: model?.storeId || undefined,
        courseName: keyword || undefined
      }),
    getRequest: (target) => getCourse(target as string | number),
    toOption: (item: any) => ({
      value: item.courseId,
      label: `${item.courseName || '未命名课程'}${item.courseNo ? `｜${item.courseNo}` : ''}${item.courseType ? `（${courseTypeNames[item.courseType] || '课程'}）` : ''}`
    })
  });
}


export async function loadCardPlanOptions({ keyword, value, model, pageNum = 1 }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    pageNum,
    value,
    listRequest: () =>
      listCardPlan({
        pageNum,
        status: model?.planStatus,
        pageSize: DEFAULT_PAGE_SIZE,
        storeId: model?.storeId || undefined,
        planName: keyword || undefined
      }),
    getRequest: (target) => getCardPlan(target as string | number),
    toOption: (item: any) => ({
      value: item.planId,
      salePrice: item.salePrice,
      label: `${item.planName || '未命名方案'}${item.planNo ? `｜${item.planNo}` : ''}${item.planType ? `（${planTypeNames[item.planType] || '课包'}）` : ''}`
    })
  });
}


export async function loadScheduleOptions({ keyword, value, model, pageNum = 1 }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    pageNum,
    value,
    listRequest: () =>
      listSchedule({
        pageNum,
        pageSize: DEFAULT_PAGE_SIZE,
        scheduleNo: keyword || undefined,
        storeId: model?.storeId || undefined,
        courseId: model?.courseId || undefined,
        coachId: model?.coachId || undefined
      }),
    getRequest: (target) => getSchedule(target as string | number),
    toOption: (item: any) => {
      const day = (item.scheduleDate || item.startTime || '').slice(0, 10);
      const times = [item.startTime?.slice(11, 16), item.endTime?.slice(11, 16)].filter(Boolean).join('–');
      return {
        value: item.scheduleId,
        label: [[day, times].filter(Boolean).join(' '), item.courseName, item.coachName, item.scheduleNo || item.scheduleId].filter(Boolean).join(' ｜ ')
      };
    }
  });
}


export async function loadMemberCardOptions({ keyword, value, model, pageNum = 1 }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    pageNum,
    value,
    listRequest: () => listMemberCard({ pageNum, pageSize: DEFAULT_PAGE_SIZE, keyword: keyword || undefined, memberId: model?.memberId || undefined, storeId: model?.storeId || undefined }),
    getRequest: (target) => getMemberCard(target as string | number),
    toOption: (item: any) => ({
      value: item.cardId,
      label: `${item.cardName || item.planName || '会员卡'}${item.memberName ? `｜${item.memberName}` : ''}${item.cardNo ? `｜${item.cardNo}` : ''}`
    })
  });
}

export async function loadSystemUserOptions({ keyword, value, pageNum = 1 }: RemoteSelectParams) {
  return buildOptions({
    keyword,
    pageNum,
    value,
    listRequest: () =>
      listUser({
        pageNum,
        pageSize: DEFAULT_PAGE_SIZE,
        status: '0',
        userName: keyword || undefined,
      }),
    getRequest: (target) => getUser(target as string | number),
    getData: (res) => res?.data?.user,
    toOption: (item: any) => ({
      value: item.userId,
      label: `${item.nickName || item.userName || '员工账号'}${item.userName && item.userName !== item.nickName ? `（${item.userName}）` : ''}`
    })
  });
}
