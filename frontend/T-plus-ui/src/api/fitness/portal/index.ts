import type { AxiosPromise } from 'axios';
import request from '@/utils/request';

export interface PortalSlogans {
  eyebrow: string;
  titleLead: string;
  titleAccent: string;
  titleSecondLine: string;
  subtitle: string;
  galleryCaption: string;
  footerCaption: string;
  version: number;
}

export const getPortalSlogans = (): AxiosPromise<PortalSlogans> =>
  request({ url: '/fitness/portal/slogans', method: 'get' });

export const updatePortalSlogans = (data: PortalSlogans): AxiosPromise<PortalSlogans> =>
  request({ url: '/fitness/portal/slogans', method: 'put', data });
