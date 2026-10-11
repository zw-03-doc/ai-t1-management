export interface FitnessStoreVO extends BaseEntity {
  storeId: number | string;
  storeCode: string;
  storeName: string;
  storeShortName?: string;
  contactName?: string;
  contactPhone?: string;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
  address?: string;
  longitude?: number;
  latitude?: number;
  businessHours?: string;
  coverUrl?: string;
  status?: string;
  remark?: string;
}

export interface FitnessStoreQuery extends PageQuery {
  storeName?: string;
  contactPhone?: string;
  status?: string;
}

export interface FitnessStoreForm {
  storeId?: number | string;
  storeCode: string;
  storeName: string;
  storeShortName?: string;
  contactName?: string;
  contactPhone?: string;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
  address?: string;
  longitude?: number;
  latitude?: number;
  businessHours?: string;
  coverUrl?: string;
  status?: string;
  remark?: string;
}
