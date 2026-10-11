import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { FitnessHomeOverviewVO } from './types';

export function getHomeOverview(): AxiosPromise<FitnessHomeOverviewVO> {
  return request({ url: '/fitness/home/overview', method: 'get' });
}
