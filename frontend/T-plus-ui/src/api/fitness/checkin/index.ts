import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { FitnessCheckinForm, FitnessCheckinQuery, FitnessCheckinVO } from './types';

export function listCheckin(query: FitnessCheckinQuery): AxiosPromise<FitnessCheckinVO[]> {
  return request({ url: '/fitness/checkin/list', method: 'get', params: query });
}

export function getCheckin(checkinId: string | number): AxiosPromise<FitnessCheckinVO> {
  return request({ url: '/fitness/checkin/' + checkinId, method: 'get' });
}

export function confirmCheckin(checkinId: string | number, data?: Partial<FitnessCheckinForm>) {
  return request({ url: `/fitness/checkin/${checkinId}/confirm`, method: 'put', data });
}

export function markCheckinException(checkinId: string | number, remark?: string) {
  return request({ url: `/fitness/checkin/${checkinId}/exception`, method: 'put', data: { remark } });
}
