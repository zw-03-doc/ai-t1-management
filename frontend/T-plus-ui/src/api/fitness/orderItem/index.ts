import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { FitnessOrderItemQuery, FitnessOrderItemVO } from './types';

export function listOrderItem(query: FitnessOrderItemQuery): AxiosPromise<FitnessOrderItemVO[]> {
  return request({ url: '/fitness/orderItem/list', method: 'get', params: query });
}

export function getOrderItem(itemId: string | number): AxiosPromise<FitnessOrderItemVO> {
  return request({ url: '/fitness/orderItem/' + itemId, method: 'get' });
}
