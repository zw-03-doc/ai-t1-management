import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { FitnessScheduleForm, FitnessScheduleQuery, FitnessScheduleVO } from './types';

export function listSchedule(query: FitnessScheduleQuery): AxiosPromise<FitnessScheduleVO[]> {
  return request({ url: '/fitness/schedule/list', method: 'get', params: query });
}

export function getSchedule(scheduleId: string | number): AxiosPromise<FitnessScheduleVO> {
  return request({ url: '/fitness/schedule/' + scheduleId, method: 'get' });
}

export function addSchedule(data: FitnessScheduleForm) {
  return request({ url: '/fitness/schedule', method: 'post', data });
}

export function updateSchedule(data: FitnessScheduleForm) {
  return request({ url: '/fitness/schedule', method: 'put', data });
}

export function cancelSchedule(scheduleId: string | number, cancelReason?: string) {
  return request({ url: `/fitness/schedule/${scheduleId}/cancel`, method: 'put', data: { cancelReason } });
}

export function delSchedule(scheduleId: string | number | Array<string | number>) {
  return request({ url: '/fitness/schedule/' + scheduleId, method: 'delete' });
}
