package org.dromara.fitness.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.domain.FitCardPlan;
import org.dromara.fitness.domain.FitCheckinRecord;
import org.dromara.fitness.domain.FitMember;
import org.dromara.fitness.domain.FitMemberCard;
import org.dromara.fitness.domain.FitOrder;
import org.dromara.fitness.domain.FitOrderItem;
import org.dromara.fitness.domain.bo.FitnessOrderBo;
import org.dromara.fitness.domain.vo.FitnessOrderVo;
import org.dromara.fitness.mapper.FitnessCardPlanMapper;
import org.dromara.fitness.mapper.FitnessCheckinMapper;
import org.dromara.fitness.mapper.FitnessMemberCardMapper;
import org.dromara.fitness.mapper.FitnessMemberMapper;
import org.dromara.fitness.mapper.FitnessOrderItemMapper;
import org.dromara.fitness.mapper.FitnessOrderMapper;
import org.dromara.fitness.service.IFitnessOrderService;
import org.dromara.fitness.utils.FitnessBizCodeUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * 订单 Service 实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessOrderServiceImpl implements IFitnessOrderService {

    private final FitnessOrderMapper baseMapper;
    private final FitnessOrderItemMapper orderItemMapper;
    private final FitnessMemberCardMapper memberCardMapper;
    private final FitnessCardPlanMapper cardPlanMapper;
    private final FitnessMemberMapper memberMapper;
    private final FitnessCheckinMapper checkinMapper;

    @Override
    public FitnessOrderVo queryById(Long orderId) {
        FitnessOrderVo vo = baseMapper.selectOrderById(orderId);
        if (vo != null) {
            vo.setItemList(baseMapper.selectOrderItemList(orderId));
        }
        return vo;
    }

    @Override
    public TableDataInfo<FitnessOrderVo> queryPageList(FitnessOrderBo bo, PageQuery pageQuery) {
        return TableDataInfo.build(baseMapper.selectPageOrderList(pageQuery.build(), bo));
    }

    @Override
    public List<FitnessOrderVo> queryList(FitnessOrderBo bo) {
        return baseMapper.selectOrderList(bo);
    }

    @Override
    @Lock4j(keys = {"'order:create:' + #bo.memberId"}, acquireTimeout = 3000)
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(FitnessOrderBo bo) {
        validBeforeCreate(bo);
        FitCardPlan plan = requirePlan(bo.getPlanId());
        BigDecimal totalAmount = nullSafe(plan.getSalePrice());
        BigDecimal discountAmount = nullSafe(bo.getDiscountAmount());
        if (discountAmount.compareTo(totalAmount) > 0) {
            throw new ServiceException("优惠金额不能大于订单总额");
        }
        BigDecimal payableAmount = totalAmount.subtract(discountAmount);
        boolean payNow = Boolean.TRUE.equals(bo.getPayNow());

        FitOrder order = MapstructUtils.convert(bo, FitOrder.class);
        order.setOrderId(IdGeneratorUtil.nextLongId());
        order.setTenantId(currentTenantId());
        order.setStoreId(bo.getStoreId() == null ? plan.getStoreId() : bo.getStoreId());
        order.setOrderNo(StringUtils.defaultIfBlank(bo.getOrderNo(), "ORD" + IdGeneratorUtil.nextId()));
        order.setOrderType(StringUtils.defaultIfBlank(bo.getOrderType(), "buy_card"));
        order.setOrderSource(StringUtils.defaultIfBlank(bo.getOrderSource(), "admin"));
        order.setOrderStatus(payNow ? "paid" : StringUtils.defaultIfBlank(bo.getOrderStatus(), "pending"));
        order.setPayMethod(payNow ? StringUtils.defaultIfBlank(bo.getPayMethod(), "cash") : bo.getPayMethod());
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayableAmount(payableAmount);
        order.setPaidAmount(payNow ? payableAmount : BigDecimal.ZERO);
        order.setPaidTime(payNow ? new Date() : null);
        validOrderNoUnique(order.getOrderNo(), null);
        if (baseMapper.insert(order) <= 0) {
            throw new ServiceException("创建订单失败");
        }

        FitOrderItem item = buildOrderItem(order, plan);
        if (orderItemMapper.insert(item) <= 0) {
            throw new ServiceException("创建订单明细失败");
        }

        if (payNow) {
            issueCardsIfNecessary(order, bo);
        }
        bo.setOrderId(order.getOrderId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(FitnessOrderBo bo) {
        FitOrder db = baseMapper.selectById(bo.getOrderId());
        if (db == null) {
            throw new ServiceException("订单不存在");
        }
        if (!Objects.equals("pending", db.getOrderStatus())) {
            throw new ServiceException("仅待支付订单支持编辑");
        }
        FitOrder update = new FitOrder();
        update.setOrderId(db.getOrderId());
        update.setStoreId(bo.getStoreId());
        update.setPayMethod(bo.getPayMethod());
        update.setTradeNo(bo.getTradeNo());
        update.setCashierUserId(bo.getCashierUserId());
        update.setRemark(bo.getRemark());

        if (bo.getDiscountAmount() != null) {
            BigDecimal totalAmount = nullSafe(db.getTotalAmount());
            if (bo.getDiscountAmount().compareTo(totalAmount) > 0) {
                throw new ServiceException("优惠金额不能大于订单总额");
            }
            update.setDiscountAmount(bo.getDiscountAmount());
            update.setPayableAmount(totalAmount.subtract(bo.getDiscountAmount()));
        }
        boolean updated = baseMapper.updateById(update) > 0;
        if (updated && bo.getDiscountAmount() != null) {
            List<FitOrderItem> itemList = orderItemMapper.selectList(new LambdaQueryWrapper<FitOrderItem>()
                .eq(FitOrderItem::getOrderId, bo.getOrderId())
                .orderByAsc(FitOrderItem::getItemId));
            if (!itemList.isEmpty()) {
                FitOrderItem item = itemList.get(0);
                FitOrderItem itemUpdate = new FitOrderItem();
                itemUpdate.setItemId(item.getItemId());
                itemUpdate.setDiscountAmount(bo.getDiscountAmount());
                itemUpdate.setRealAmount(nullSafe(item.getTotalPrice()).subtract(bo.getDiscountAmount()));
                orderItemMapper.updateById(itemUpdate);
            }
        }
        return updated;
    }

    @Override
    @Lock4j(keys = {"'order:pay:' + #orderId"}, acquireTimeout = 3000)
    @Transactional(rollbackFor = Exception.class)
    public Boolean payOrder(Long orderId, FitnessOrderBo bo) {
        FitOrder db = baseMapper.selectById(orderId);
        if (db == null) {
            throw new ServiceException("订单不存在");
        }
        if (Objects.equals("paid", db.getOrderStatus())) {
            return true;
        }
        if (!Objects.equals("pending", db.getOrderStatus())) {
            throw new ServiceException("当前订单状态不支持支付");
        }
        FitOrder update = new FitOrder();
        update.setOrderId(orderId);
        update.setOrderStatus("paid");
        update.setPayMethod(StringUtils.defaultIfBlank(bo.getPayMethod(), StringUtils.defaultIfBlank(db.getPayMethod(), "cash")));
        update.setPaidAmount(nullSafe(db.getPayableAmount()));
        update.setPaidTime(new Date());
        update.setTradeNo(StringUtils.isNotBlank(bo.getTradeNo()) ? bo.getTradeNo() : db.getTradeNo());
        update.setCashierUserId(bo.getCashierUserId() != null ? bo.getCashierUserId() : db.getCashierUserId());
        boolean flag = baseMapper.updateById(update) > 0;
        if (flag) {
            FitOrder latest = baseMapper.selectById(orderId);
            issueCardsIfNecessary(latest, bo);
        }
        return flag;
    }

    @Override
    public Boolean cancelOrder(Long orderId, String cancelReason) {
        FitOrder db = baseMapper.selectById(orderId);
        if (db == null) {
            throw new ServiceException("订单不存在");
        }
        if (Objects.equals("cancelled", db.getOrderStatus())) {
            return true;
        }
        if (!Objects.equals("pending", db.getOrderStatus())) {
            throw new ServiceException("仅待支付订单支持取消");
        }
        FitOrder update = new FitOrder();
        update.setOrderId(orderId);
        update.setOrderStatus("cancelled");
        if (StringUtils.isNotBlank(cancelReason)) {
            update.setRemark(cancelReason);
        }
        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Lock4j(keys = {"'order:refund:' + #orderId"}, acquireTimeout = 5000)
    @Transactional(rollbackFor = Exception.class)
    public Boolean refundOrder(Long orderId, FitnessOrderBo bo) {
        FitnessOrderBo refundBo = bo == null ? new FitnessOrderBo() : bo;
        FitOrder db = baseMapper.selectById(orderId);
        if (db == null) {
            throw new ServiceException("订单不存在");
        }
        if (Objects.equals("refunded", db.getOrderStatus())) {
            return true;
        }
        if (!Objects.equals("paid", db.getOrderStatus())) {
            throw new ServiceException("仅已支付订单支持退款");
        }
        BigDecimal paidAmount = nullSafe(db.getPaidAmount());
        BigDecimal refundAmount = refundBo.getRefundAmount() == null ? paidAmount : nullSafe(refundBo.getRefundAmount());
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("退款金额必须大于0");
        }
        if (refundAmount.compareTo(paidAmount) != 0) {
            throw new ServiceException("当前版本退款策略已定稿为仅支持全额退款，部分退款需等待退款单/流水模块上线");
        }

        List<FitOrderItem> itemList = orderItemMapper.selectList(new LambdaQueryWrapper<FitOrderItem>()
            .eq(FitOrderItem::getOrderId, orderId)
            .orderByAsc(FitOrderItem::getItemId));
        rollbackGrantedCards(orderId, itemList);

        FitOrder update = new FitOrder();
        update.setOrderId(orderId);
        update.setOrderStatus("refunded");
        update.setRemark(appendRefundRemark(db.getRemark(), refundBo.getRemark(), refundAmount));
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {

        if (isValid) {
            long count = baseMapper.selectCount(new LambdaQueryWrapper<FitOrder>().in(FitOrder::getOrderId, ids));
            if (count != ids.size()) {
                throw new ServiceException("部分订单不存在或已被删除");
            }
            long paidCount = baseMapper.selectCount(new LambdaQueryWrapper<FitOrder>()
                .in(FitOrder::getOrderId, ids)
                .eq(FitOrder::getOrderStatus, "paid"));
            if (paidCount > 0) {
                throw new ServiceException("已支付订单不允许直接删除");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    private void validBeforeCreate(FitnessOrderBo bo) {
        long memberCount = memberMapper.selectCount(new LambdaQueryWrapper<FitMember>().eq(FitMember::getMemberId, bo.getMemberId()));
        if (memberCount <= 0) {
            throw new ServiceException("会员不存在");
        }
        requirePlan(bo.getPlanId());
    }

    private FitCardPlan requirePlan(Long planId) {
        FitCardPlan plan = cardPlanMapper.selectById(planId);
        if (plan == null) {
            throw new ServiceException("卡项方案不存在");
        }
        if (!Objects.equals("0", plan.getStatus())) {
            throw new ServiceException("卡项方案未上架，无法录单");
        }
        return plan;
    }

    private FitOrderItem buildOrderItem(FitOrder order, FitCardPlan plan) {
        FitOrderItem item = new FitOrderItem();
        item.setItemId(IdGeneratorUtil.nextLongId());
        item.setItemNo(FitnessBizCodeUtils.defaultIfBlank(null, "ITM"));
        item.setTenantId(order.getTenantId());

        item.setOrderId(order.getOrderId());
        item.setItemType("card_plan");
        item.setItemRefId(plan.getPlanId());
        item.setItemName(plan.getPlanName());
        item.setQuantity(1);
        item.setUnitPrice(nullSafe(plan.getSalePrice()));
        item.setTotalPrice(nullSafe(order.getTotalAmount()));
        item.setDiscountAmount(nullSafe(order.getDiscountAmount()));
        item.setRealAmount(nullSafe(order.getPayableAmount()));
        return item;
    }

    private void issueCardsIfNecessary(FitOrder order, FitnessOrderBo bo) {
        List<FitOrderItem> itemList = orderItemMapper.selectList(new LambdaQueryWrapper<FitOrderItem>()
            .eq(FitOrderItem::getOrderId, order.getOrderId())
            .orderByAsc(FitOrderItem::getItemId));
        for (FitOrderItem item : itemList) {
            if (!Objects.equals("card_plan", item.getItemType()) || item.getGrantCardId() != null) {
                continue;
            }
            FitCardPlan plan = requirePlan(item.getItemRefId());
            if (Objects.equals("1", plan.getIsPrivateBindCoach()) && bo.getBindCoachId() == null) {
                throw new ServiceException("该卡项要求绑定教练");
            }
            Long cardId = issueMemberCard(order, item, plan, bo.getBindCoachId());
            FitOrderItem updateItem = new FitOrderItem();
            updateItem.setItemId(item.getItemId());
            updateItem.setGrantCardId(cardId);
            orderItemMapper.updateById(updateItem);
        }
    }

    private Long issueMemberCard(FitOrder order, FitOrderItem item, FitCardPlan plan, Long bindCoachId) {
        Date activeTime = order.getPaidTime() == null ? new Date() : order.getPaidTime();
        Date expireTime = plan.getValidDays() == null || plan.getValidDays() <= 0 ? null : DateUtil.offsetDay(activeTime, plan.getValidDays());
        int totalTimes = nullSafe(plan.getTotalTimes()) + nullSafe(plan.getGiftTimes());

        FitMemberCard card = new FitMemberCard();
        card.setCardId(IdGeneratorUtil.nextLongId());
        card.setTenantId(order.getTenantId());
        card.setStoreId(order.getStoreId());
        card.setMemberId(order.getMemberId());
        card.setPlanId(plan.getPlanId());
        card.setCardNo("CARD" + IdGeneratorUtil.nextId());
        card.setCardName(plan.getPlanName());
        card.setPlanType(plan.getPlanType());
        card.setBindCoachId(bindCoachId);
        card.setActiveTime(activeTime);
        card.setExpireTime(expireTime);
        card.setTotalTimes(totalTimes);
        card.setUsedTimes(0);
        card.setRemainTimes(totalTimes);
        card.setBalanceAmount(nullSafe(plan.getStoredAmount()));
        card.setCardStatus("active");
        card.setOrderId(order.getOrderId());
        card.setRemark(order.getRemark());
        if (memberCardMapper.insert(card) <= 0) {
            throw new ServiceException("支付成功后发卡失败");
        }
        return card.getCardId();
    }

    private void rollbackGrantedCards(Long orderId, List<FitOrderItem> itemList) {
        Set<Long> grantCardIds = new HashSet<>();
        for (FitOrderItem item : itemList) {
            if (item.getGrantCardId() != null) {
                grantCardIds.add(item.getGrantCardId());
            }
        }
        List<FitMemberCard> orderCardList = memberCardMapper.selectList(new LambdaQueryWrapper<FitMemberCard>()
            .eq(FitMemberCard::getOrderId, orderId));
        for (FitMemberCard card : orderCardList) {
            grantCardIds.add(card.getCardId());
        }
        if (grantCardIds.isEmpty()) {
            return;
        }

        for (Long cardId : grantCardIds) {
            FitMemberCard card = memberCardMapper.selectById(cardId);
            if (card == null) {
                continue;
            }
            if (!Objects.equals(card.getOrderId(), orderId)) {
                throw new ServiceException("订单发卡记录与会员卡来源订单不一致，禁止退款");
            }
            if (nullSafe(card.getUsedTimes()) > 0) {
                throw new ServiceException("会员卡已产生扣次记录，请先完成核销撤销后再退款");
            }
            long successCheckinCount = checkinMapper.selectCount(new LambdaQueryWrapper<FitCheckinRecord>()
                .eq(FitCheckinRecord::getCardId, cardId)
                .eq(FitCheckinRecord::getCheckinStatus, "success"));
            if (successCheckinCount > 0) {
                throw new ServiceException("会员卡已产生核销记录，请先完成核销撤销后再退款");
            }
            memberCardMapper.deleteById(cardId);
        }

        orderItemMapper.update(null, new LambdaUpdateWrapper<FitOrderItem>()
            .set(FitOrderItem::getGrantCardId, null)
            .eq(FitOrderItem::getOrderId, orderId)
            .in(FitOrderItem::getGrantCardId, grantCardIds));
    }

    private String appendRefundRemark(String originalRemark, String refundRemark, BigDecimal refundAmount) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotBlank(originalRemark)) {
            builder.append(originalRemark).append("\n");
        }
        builder.append("全额退款：").append(refundAmount);
        if (StringUtils.isNotBlank(refundRemark)) {
            builder.append("，原因：").append(refundRemark);
        }
        return builder.toString();
    }

    private void validOrderNoUnique(String orderNo, Long excludeOrderId) {
        long count = baseMapper.selectCount(new LambdaQueryWrapper<FitOrder>()
            .eq(FitOrder::getOrderNo, orderNo)
            .ne(excludeOrderId != null, FitOrder::getOrderId, excludeOrderId));
        if (count > 0) {
            throw new ServiceException("订单号已存在");
        }
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private String currentTenantId() {
        return StringUtils.defaultIfBlank(TenantHelper.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
    }
}
