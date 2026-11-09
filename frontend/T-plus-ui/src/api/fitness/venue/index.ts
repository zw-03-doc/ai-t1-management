import type { AxiosPromise } from 'axios';
import request from '@/utils/request';

export interface VenuePhoto { id: string; url: string }
export interface VenueGallery { version: number; photos: VenuePhoto[] }
const base = '/fitness/portal/venue';

export const getGallery = (): AxiosPromise<VenueGallery> => request({ url: base, method: 'get' });

export function uploadPhoto(version: number, file: File, replaceId?: string): AxiosPromise<VenueGallery> {
  const data = new FormData();
  data.append('version', String(version));
  data.append('file', file);
  if (replaceId) data.append('replaceId', replaceId);
  return request({ url: base + '/images', method: 'post', data, headers: { repeatSubmit: false }, timeout: 90000 });
}

export const reorderPhotos = (version: number, ids: string[]): AxiosPromise<VenueGallery> =>
  request({ url: base + '/order', method: 'put', data: { version, ids } });

export const deletePhoto = (version: number, id: string): AxiosPromise<VenueGallery> =>
  request({ url: base + '/images/' + encodeURIComponent(id), method: 'delete', params: { version } });
