import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import {
  FitnessCoachStoreReportVO,
  FitnessCourseReportVO,
  FitnessMemberReportVO,
  FitnessOrderReportVO,
  FitnessReportQuery
} from './types';

export function listMemberReport(query: FitnessReportQuery): AxiosPromise<FitnessMemberReportVO[]> {
  return request({ url: '/fitness/report/member', method: 'get', params: query });
}

export function listOrderReport(query: FitnessReportQuery): AxiosPromise<FitnessOrderReportVO[]> {
  return request({ url: '/fitness/report/order', method: 'get', params: query });
}

export function listCourseReport(query: FitnessReportQuery): AxiosPromise<FitnessCourseReportVO[]> {
  return request({ url: '/fitness/report/course', method: 'get', params: query });
}

export function listCoachStoreReport(query: FitnessReportQuery): AxiosPromise<FitnessCoachStoreReportVO[]> {
  return request({ url: '/fitness/report/coachStore', method: 'get', params: query });
}
