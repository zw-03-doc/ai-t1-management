import request from '@/utils/request';
import { FitnessActivityForm, FitnessActivityQuery, FitnessActivityVO } from './types';
import { AxiosPromise } from 'axios';

export function listActivity(query: FitnessActivityQuery): AxiosPromise<FitnessActivityVO[]> {
  return request({ url: '/fitness/activity/list', method: 'get', params: query });
}

export function getActivity(activityId: string | number): AxiosPromise<FitnessActivityVO> {
  return request({ url: '/fitness/activity/' + activityId, method: 'get' });
}

export function addActivity(data: FitnessActivityForm) {
  return request({ url: '/fitness/activity', method: 'post', data });
}

export function updateActivity(data: FitnessActivityForm) {
  return request({ url: '/fitness/activity', method: 'put', data });
}

export function changeActivityStatus(activityId: string | number, status: string) {
  return request({ url: `/fitness/activity/${activityId}/status`, method: 'put', data: { status } });
}

export function delActivity(activityId: string | number | Array<string | number>) {
  return request({ url: '/fitness/activity/' + activityId, method: 'delete' });
}
