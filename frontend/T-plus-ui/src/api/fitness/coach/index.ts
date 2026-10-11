import request from '@/utils/request';
import { FitnessCoachForm, FitnessCoachQuery, FitnessCoachVO } from './types';
import { AxiosPromise } from 'axios';

export function listCoach(query: FitnessCoachQuery): AxiosPromise<FitnessCoachVO[]> {
  return request({ url: '/fitness/coach/list', method: 'get', params: query });
}

export function getCoach(coachId: string | number): AxiosPromise<FitnessCoachVO> {
  return request({ url: '/fitness/coach/' + coachId, method: 'get' });
}

export function addCoach(data: FitnessCoachForm) {
  return request({ url: '/fitness/coach', method: 'post', data });
}

export function updateCoach(data: FitnessCoachForm) {
  return request({ url: '/fitness/coach', method: 'put', data });
}

export function delCoach(coachId: string | number | Array<string | number>) {
  return request({ url: '/fitness/coach/' + coachId, method: 'delete' });
}
