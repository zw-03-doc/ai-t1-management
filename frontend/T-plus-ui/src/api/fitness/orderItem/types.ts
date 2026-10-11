export interface FitnessOrderItemVO extends BaseEntity {
  itemId: number | string;
  itemNo?: string;
  orderId?: number | string;
  orderNo?: string;
  orderStatus?: string;
  storeId?: number | string;
  storeName?: string;
  memberId?: number | string;
  memberName?: string;
  memberMobile?: string;
  itemType?: string;
  itemRefId?: number | string;
  itemName?: string;
  quantity?: number;
  unitPrice?: number;
  totalPrice?: number;
  discountAmount?: number;
  realAmount?: number;
  grantCardId?: number | string;
  remark?: string;
}

export interface FitnessOrderItemQuery extends PageQuery {
  orderId?: number | string;
  orderNo?: string;
  storeId?: number | string;
  memberId?: number | string;
  itemType?: string;
  itemName?: string;
  orderCreateTimeStart?: string;
  orderCreateTimeEnd?: string;
}
