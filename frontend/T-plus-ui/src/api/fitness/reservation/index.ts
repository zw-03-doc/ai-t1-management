import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { FitnessReservationForm, FitnessReservationQuery, FitnessReservationVO } from './types';

export function listReservation(query: FitnessReservationQuery): AxiosPromise<FitnessReservationVO[]> {
  return request({ url: '/fitness/reservation/list', method: 'get', params: query });
}

export function getReservation(reservationId: string | number): AxiosPromise<FitnessReservationVO> {
  return request({ url: '/fitness/reservation/' + reservationId, method: 'get' });
}

export function addReservation(data: FitnessReservationForm) {
  return request({ url: '/fitness/reservation', method: 'post', data });
}

export function updateReservation(data: FitnessReservationForm) {
  return request({ url: '/fitness/reservation', method: 'put', data });
}

export function cancelReservation(reservationId: string | number, cancelReason?: string) {
  return request({ url: `/fitness/reservation/${reservationId}/cancel`, method: 'put', data: { cancelReason } });
}

export function delReservation(reservationId: string | number | Array<string | number>) {
  return request({ url: '/fitness/reservation/' + reservationId, method: 'delete' });
}
