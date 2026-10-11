import request from '@/utils/request';
import { FitnessStoreForm, FitnessStoreQuery, FitnessStoreVO } from './types';
import { AxiosPromise } from 'axios';

export function listStore(query: FitnessStoreQuery): AxiosPromise<FitnessStoreVO[]> {
  return request({ url: '/fitness/store/list', method: 'get', params: query });
}

export function getStore(storeId: string | number): AxiosPromise<FitnessStoreVO> {
  return request({ url: '/fitness/store/' + storeId, method: 'get' });
}

export function addStore(data: FitnessStoreForm) {
  return request({ url: '/fitness/store', method: 'post', data });
}

export function updateStore(data: FitnessStoreForm) {
  return request({ url: '/fitness/store', method: 'put', data });
}

export function changeStoreStatus(storeId: string | number, status: string) {
  return request({ url: `/fitness/store/${storeId}/status`, method: 'put', data: { status } });
}

export function delStore(storeId: string | number | Array<string | number>) {
  return request({ url: '/fitness/store/' + storeId, method: 'delete' });
}
