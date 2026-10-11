export interface FitnessOrderItemVO {
  itemId?: number | string;
  itemType?: string;
  itemRefId?: number | string;
  itemName?: string;
  itemPrice?: number;
  quantity?: number;
  subtotalAmount?: number;
  remark?: string;
}

export interface FitnessOrderVO extends BaseEntity {
  orderId: number | string;
  orderNo?: string;
  storeId?: number | string;
  storeName?: string;
  memberId?: number | string;
  memberName?: string;
  memberMobile?: string;
  orderType?: string;
  orderSource?: string;
  orderStatus?: string;
  payMethod?: string;
  totalAmount?: number;
  discountAmount?: number;
  payableAmount?: number;
  paidAmount?: number;
  refundAmount?: number;
  paidTime?: string;
  tradeNo?: string;
  cashierUserId?: number | string;
  remark?: string;
  itemList?: FitnessOrderItemVO[];
}

export interface FitnessOrderQuery extends PageQuery {
  orderNo?: string;
  memberId?: number | string;
  storeId?: number | string;
  orderType?: string;
  orderStatus?: string;
}

export interface FitnessOrderForm {
  orderId?: number | string;
  storeId?: number | string;
  memberId?: number | string;
  planId?: number | string;
  bindCoachId?: number | string;
  orderNo?: string;
  orderType?: string;
  orderSource?: string;
  orderStatus?: string;
  payMethod?: string;
  discountAmount?: number;
  payableAmount?: number;
  paidAmount?: number;
  refundAmount?: number;
  tradeNo?: string;
  cashierUserId?: number | string;
  payNow?: boolean;
  remark?: string;
}
