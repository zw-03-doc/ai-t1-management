import request from '@/utils/request';
import { FitnessCardPlanForm, FitnessCardPlanQuery, FitnessCardPlanVO } from './types';
import { AxiosPromise } from 'axios';

export function listCardPlan(query: FitnessCardPlanQuery): AxiosPromise<FitnessCardPlanVO[]> {
  return request({ url: '/fitness/cardPlan/list', method: 'get', params: query });
}

export function getCardPlan(planId: string | number): AxiosPromise<FitnessCardPlanVO> {
  return request({ url: '/fitness/cardPlan/' + planId, method: 'get' });
}

export function addCardPlan(data: FitnessCardPlanForm) {
  return request({ url: '/fitness/cardPlan', method: 'post', data });
}

export function updateCardPlan(data: FitnessCardPlanForm) {
  return request({ url: '/fitness/cardPlan', method: 'put', data });
}

export function changeCardPlanStatus(planId: string | number, status: string) {
  return request({ url: `/fitness/cardPlan/${planId}/status`, method: 'put', data: { status } });
}

export function delCardPlan(planId: string | number | Array<string | number>) {
  return request({ url: '/fitness/cardPlan/' + planId, method: 'delete' });
}
