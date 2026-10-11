import request from '@/utils/request';
import { FitnessCourseForm, FitnessCourseQuery, FitnessCourseVO } from './types';
import { AxiosPromise } from 'axios';

export function listCourse(query: FitnessCourseQuery): AxiosPromise<FitnessCourseVO[]> {
  return request({ url: '/fitness/course/list', method: 'get', params: query });
}

export function getCourse(courseId: string | number): AxiosPromise<FitnessCourseVO> {
  return request({ url: '/fitness/course/' + courseId, method: 'get' });
}

export function addCourse(data: FitnessCourseForm) {
  return request({ url: '/fitness/course', method: 'post', data });
}

export function updateCourse(data: FitnessCourseForm) {
  return request({ url: '/fitness/course', method: 'put', data });
}

export function changeCourseStatus(courseId: string | number, status: string) {
  return request({ url: `/fitness/course/${courseId}/status`, method: 'put', data: { status } });
}

export function delCourse(courseId: string | number | Array<string | number>) {
  return request({ url: '/fitness/course/' + courseId, method: 'delete' });
}
