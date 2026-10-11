import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { FitnessOrderForm, FitnessOrderQuery, FitnessOrderVO } from './types';

export function listOrder(query: FitnessOrderQuery): AxiosPromise<FitnessOrderVO[]> {
  return request({ url: '/fitness/order/list', method: 'get', params: query });
}

export function getOrder(orderId: string | number): AxiosPromise<FitnessOrderVO> {
  return request({ url: '/fitness/order/' + orderId, method: 'get' });
}

export function addOrder(data: FitnessOrderForm) {
  return request({ url: '/fitness/order', method: 'post', data });
}

export function updateOrder(data: FitnessOrderForm) {
  return request({ url: '/fitness/order', method: 'put', data });
}

export function payOrder(orderId: string | number, data?: Partial<FitnessOrderForm>) {
  return request({ url: `/fitness/order/${orderId}/pay`, method: 'put', data });
}

export function cancelOrder(orderId: string | number, remark?: string) {
  return request({ url: `/fitness/order/${orderId}/cancel`, method: 'put', data: { remark } });
}

export function refundOrder(orderId: string | number, data?: Partial<FitnessOrderForm>) {
  return request({ url: `/fitness/order/${orderId}/refund`, method: 'put', data });
}

export function delOrder(orderId: string | number | Array<string | number>) {
  return request({ url: '/fitness/order/' + orderId, method: 'delete' });
}
