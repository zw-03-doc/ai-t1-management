import request from '@/utils/request';
import { FitnessMemberForm, FitnessMemberQuery, FitnessMemberVO, FitnessMemberDetailVO } from './types';
import { AxiosPromise } from 'axios';

export function listMember(query: FitnessMemberQuery): AxiosPromise<FitnessMemberVO[]> {
  return request({ url: '/fitness/member/list', method: 'get', params: query });
}

export function getMember(memberId: string | number): AxiosPromise<FitnessMemberVO> {
  return request({ url: '/fitness/member/' + memberId, method: 'get' });
}

export function getMemberDetail(memberId: string | number): AxiosPromise<FitnessMemberDetailVO> {
  return request({ url: '/fitness/member/' + memberId + '/detail', method: 'get' });
}

export function addMember(data: FitnessMemberForm) {
  return request({ url: '/fitness/member', method: 'post', data });
}

export function updateMember(data: FitnessMemberForm) {
  return request({ url: '/fitness/member', method: 'put', data });
}

export function delMember(memberId: string | number | Array<string | number>) {
  return request({ url: '/fitness/member/' + memberId, method: 'delete' });
}
