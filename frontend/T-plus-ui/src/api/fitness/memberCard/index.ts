import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { FitnessMemberCardForm, FitnessMemberCardQuery, FitnessMemberCardVO } from './types';

export function bindMemberCard(data: { memberId: string | number; planId: string | number; storeId: string | number; bindCoachId?: string | number; requestId: string; remark?: string }): AxiosPromise<{ cardId: string; cardNo: string; created: boolean }> {
  return request({ url: '/fitness/memberCard/bind', method: 'post', data });
}

export function listMemberCard(query: FitnessMemberCardQuery): AxiosPromise<FitnessMemberCardVO[]> {
  return request({ url: '/fitness/memberCard/list', method: 'get', params: query });
}

export function getMemberCard(cardId: string | number): AxiosPromise<FitnessMemberCardVO> {
  return request({ url: '/fitness/memberCard/' + cardId, method: 'get' });
}

export function changeMemberCardStatus(cardId: string | number, cardStatus: string) {
  return request({ url: `/fitness/memberCard/${cardId}/status`, method: 'put', data: { cardStatus } });
}

export function adjustMemberCard(cardId: string | number, data: Partial<FitnessMemberCardForm>) {
  return request({ url: `/fitness/memberCard/${cardId}/adjust`, method: 'put', data });
}
