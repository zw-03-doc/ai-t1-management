import type { AxiosPromise } from 'axios';
import request from '@/utils/request';

export interface VenuePhoto { id: string; url: string }
export interface VenueStore { storeId: string; storeName: string; status: string }
export interface VenueGallery { storeId: string; storeName: string; version: number; photos: VenuePhoto[] }
const base = '/fitness/portal/venue';

export const getVenueStores = (): AxiosPromise<VenueStore[]> => request({ url: base + '/stores', method: 'get' });
export const getGallery = (storeId: string): AxiosPromise<VenueGallery> => request({ url: base, method: 'get', params: { storeId } });
export const getPrivateImage = (storeId: string, id: string): Promise<Blob> =>
  request.get<Blob, Blob>(base + '/images/' + encodeURIComponent(id), { params: { storeId }, responseType: 'blob' });

export function uploadPhoto(storeId: string, version: number, file: File, replaceId?: string): AxiosPromise<VenueGallery> {
  const data = new FormData();
  data.append('version', String(version));
  data.append('file', file);
  if (replaceId) data.append('replaceId', replaceId);
  return request({ url: base + '/images', method: 'post', params: { storeId }, data, headers: { repeatSubmit: false }, timeout: 90000 });
}

export const reorderPhotos = (storeId: string, version: number, ids: string[]): AxiosPromise<VenueGallery> =>
  request({ url: base + '/order', method: 'put', params: { storeId }, data: { version, ids } });

export const deletePhoto = (storeId: string, version: number, id: string): AxiosPromise<VenueGallery> =>
  request({ url: base + '/images/' + encodeURIComponent(id), method: 'delete', params: { storeId, version } });
