package org.dromara.fitness.service.impl;

import org.dromara.fitness.service.support.FitnessCardIssuer;
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
    private final FitnessCardIssuer cardIssuer;

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
        BigDecimal discountAmount = orderDiscount(totalAmount, bo);
        BigDecimal payableAmount = totalAmount.subtract(discountAmount);
        boolean payNow = Boolean.TRUE.equals(bo.getPayNow());
        if (StringUtils.isNotBlank(bo.getOrderStatus()) && !"pending".equals(bo.getOrderStatus()))
            throw new ServiceException("新订单只能待收款；请使用确认线下收款完成发卡");
        requirePayMethod(bo.getPayMethod());
        Long storeId = bo.getStoreId() == null ? plan.getStoreId() : bo.getStoreId();
        cardIssuer.validateAssignment(currentTenantId(), storeId, bo.getBindCoachId(), plan);

        FitOrder order = MapstructUtils.convert(bo, FitOrder.class);
        order.setOrderId(IdGeneratorUtil.nextLongId());
        order.setTenantId(currentTenantId());
        order.setStoreId(storeId);
        order.setBindCoachId(bo.getBindCoachId());
        order.setOrderNo(StringUtils.defaultIfBlank(bo.getOrderNo(), "ORD" + IdGeneratorUtil.nextId()));
        order.setOrderType(StringUtils.defaultIfBlank(bo.getOrderType(), "buy_card"));
        order.setOrderSource(StringUtils.defaultIfBlank(bo.getOrderSource(), "admin"));
        order.setOrderStatus(payNow ? "paid" : "pending");
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
            issueCardsIfNecessary(order);
        }
        bo.setOrderId(order.getOrderId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(FitnessOrderBo bo) {
        // Match payment's lock order: order first, then any referenced coach.
        // A delayed editor must observe a completed payment before changing its card binding or amount.
        FitOrder db = baseMapper.selectOne(new LambdaQueryWrapper<FitOrder>()
            .eq(FitOrder::getOrderId, bo.getOrderId()).last("FOR UPDATE"));
        if (db == null) {
            throw new ServiceException("订单不存在");
        }
        if (!Objects.equals("pending", db.getOrderStatus())) {
            throw new ServiceException("仅待支付订单支持编辑");
        }
        if (bo.getMemberId() != null && !Objects.equals(bo.getMemberId(), db.getMemberId()))
            throw new ServiceException("订单会员不能修改，请取消后重新录单");
        var items = orderItemMapper.selectList(new LambdaQueryWrapper<FitOrderItem>().eq(FitOrderItem::getOrderId, db.getOrderId()));
        Long planId = items.stream().filter(i -> "card_plan".equals(i.getItemType())).map(FitOrderItem::getItemRefId).findFirst()
            .orElseThrow(() -> new ServiceException("订单缺少卡项明细，请联系管理员"));
        if (bo.getPlanId() != null && !Objects.equals(bo.getPlanId(), planId)) throw new ServiceException("卡项方案不能修改，请取消后重新录单");
        Long storeId = bo.getStoreId() == null ? db.getStoreId() : bo.getStoreId();
        cardIssuer.validateAssignment(currentTenantId(), storeId, bo.getBindCoachId(), requirePlan(planId));
        requirePayMethod(bo.getPayMethod());
        FitOrder update = new FitOrder();
        update.setOrderId(db.getOrderId());
        update.setStoreId(storeId);
        update.setPayMethod(bo.getPayMethod());
        update.setTradeNo(bo.getTradeNo());
        update.setCashierUserId(bo.getCashierUserId());
        update.setRemark(bo.getRemark());

        BigDecimal totalAmount = nullSafe(db.getTotalAmount());
        if (bo.getSettlementAmount() != null || bo.getQuotedPlanPrice() != null || bo.getDiscountAmount() != null) {
            BigDecimal discount = orderDiscount(totalAmount, bo);
            if (discount.compareTo(nullSafe(db.getDiscountAmount())) != 0) {
                update.setDiscountAmount(discount);
                update.setPayableAmount(totalAmount.subtract(discount));
            }
        }
        boolean updated = baseMapper.update(update, new LambdaUpdateWrapper<FitOrder>().eq(FitOrder::getOrderId, db.getOrderId())
            .set(FitOrder::getBindCoachId, bo.getBindCoachId())) > 0;
        if (updated && update.getDiscountAmount() != null) {
            List<FitOrderItem> itemList = orderItemMapper.selectList(new LambdaQueryWrapper<FitOrderItem>()
                .eq(FitOrderItem::getOrderId, bo.getOrderId())
                .orderByAsc(FitOrderItem::getItemId));
            if (itemList.size() != 1) throw new ServiceException("请核对订单明细后再调整金额");
            FitOrderItem itemUpdate = new FitOrderItem();
            itemUpdate.setItemId(itemList.get(0).getItemId());
            itemUpdate.setDiscountAmount(update.getDiscountAmount());
            itemUpdate.setRealAmount(update.getPayableAmount());
            if (orderItemMapper.updateById(itemUpdate) != 1) throw new ServiceException("订单明细更新失败");
        }
        return updated;
    }

    @Override
    @Lock4j(keys = {"'order:pay:' + #orderId"}, acquireTimeout = 3000)
    @Transactional(rollbackFor = Exception.class)
    public Boolean payOrder(Long orderId, FitnessOrderBo bo) {
        FitOrder db = baseMapper.selectOne(new LambdaQueryWrapper<FitOrder>().eq(FitOrder::getOrderId, orderId).last("FOR UPDATE"));
        if (db == null) {
            throw new ServiceException("订单不存在");
        }
        if (Objects.equals("paid", db.getOrderStatus())) {
            return true;
        }
        if (!Objects.equals("pending", db.getOrderStatus())) {
            throw new ServiceException("当前订单状态不支持支付");
        }
        requireActiveMember(db.getMemberId());
        if (bo.getPayableAmount() != null && bo.getPayableAmount().compareTo(nullSafe(db.getPayableAmount())) != 0)
            throw new ServiceException("应收金额已变化，请重新打开订单核对后确认");
        requirePayMethod(bo.getPayMethod());
        Long bindCoachId = bo.getBindCoachId() == null ? db.getBindCoachId() : bo.getBindCoachId();
        FitOrder update = new FitOrder();
        update.setOrderId(orderId);
        update.setBindCoachId(bindCoachId);
        update.setOrderStatus("paid");
        update.setPayMethod(StringUtils.defaultIfBlank(bo.getPayMethod(), StringUtils.defaultIfBlank(db.getPayMethod(), "cash")));
        update.setPaidAmount(nullSafe(db.getPayableAmount()));
        update.setPaidTime(new Date());
        update.setTradeNo(StringUtils.isNotBlank(bo.getTradeNo()) ? bo.getTradeNo() : db.getTradeNo());
        update.setCashierUserId(bo.getCashierUserId() != null ? bo.getCashierUserId() : db.getCashierUserId());
        boolean flag = baseMapper.updateById(update) > 0;
        if (flag) {
            FitOrder latest = baseMapper.selectById(orderId);
            latest.setBindCoachId(bindCoachId);
            issueCardsIfNecessary(latest);
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelOrder(Long orderId, String cancelReason) {
        FitOrder db = baseMapper.selectOne(new LambdaQueryWrapper<FitOrder>()
            .eq(FitOrder::getOrderId, orderId).last("FOR UPDATE"));
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
        FitOrder db = baseMapper.selectOne(new LambdaQueryWrapper<FitOrder>()
            .eq(FitOrder::getOrderId, orderId).last("FOR UPDATE"));
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
        requireActiveMember(bo.getMemberId());
        requirePlan(bo.getPlanId());
    }

    private void requireActiveMember(Long memberId) {
        FitMember member = memberMapper.selectOne(new LambdaQueryWrapper<FitMember>().eq(FitMember::getMemberId, memberId).last("FOR UPDATE"));
        if (member == null || !Objects.equals(member.getTenantId(), currentTenantId()) || !"normal".equals(member.getMemberStatus()))
            throw new ServiceException("会员不存在或已停用");
    }

    private void requirePayMethod(String method) {
        if (StringUtils.isNotBlank(method) && !Set.of("cash", "wechat", "transfer", "card", "mix").contains(method))
            throw new ServiceException("请选择有效的线下收款方式");
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

    private BigDecimal orderDiscount(BigDecimal total, FitnessOrderBo bo) {
        if (bo.getQuotedPlanPrice() != null && bo.getQuotedPlanPrice().compareTo(total) != 0)
            throw new ServiceException("卡项价格已变化，请重新选择方案并核对金额");
        BigDecimal amount = bo.getSettlementAmount();
        if (amount != null) {
            if (amount.signum() < 0 || amount.compareTo(total) > 0 || amount.stripTrailingZeros().scale() > 2)
                throw new ServiceException("实付金额须在 0 元与应付金额之间，最多两位小数");
            return total.subtract(amount);
        }
        if (bo.getQuotedPlanPrice() != null) throw new ServiceException("请输入实付金额");
        BigDecimal discount = nullSafe(bo.getDiscountAmount());
        if (discount.signum() < 0 || discount.compareTo(total) > 0) throw new ServiceException("优惠金额超出订单金额范围");
        return discount;
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

    private void issueCardsIfNecessary(FitOrder order) {
        List<FitOrderItem> itemList = orderItemMapper.selectList(new LambdaQueryWrapper<FitOrderItem>()
            .eq(FitOrderItem::getOrderId, order.getOrderId())
            .orderByAsc(FitOrderItem::getItemId));
        for (FitOrderItem item : itemList) {
            if (!Objects.equals("card_plan", item.getItemType()) || item.getGrantCardId() != null) {
                continue;
            }
            FitCardPlan plan = requirePlan(item.getItemRefId());
            cardIssuer.validateAssignment(currentTenantId(), order.getStoreId(), order.getBindCoachId(), plan);
            Date activated = order.getPaidTime() == null ? new Date() : order.getPaidTime();
            Long cardId = cardIssuer.issue(plan, order.getTenantId(), order.getMemberId(), order.getStoreId(), order.getBindCoachId(),
                activated, "CARD" + IdGeneratorUtil.nextId(), order.getOrderId(), order.getRemark()).getCardId();
            FitOrderItem updateItem = new FitOrderItem();
            updateItem.setItemId(item.getItemId());
            updateItem.setGrantCardId(cardId);
            orderItemMapper.updateById(updateItem);
        }
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

        // Redemption locks reservation -> card -> checkin, never order. Keep the
        // same card -> checkin order here, with stable card ordering for batches.
        for (Long cardId : grantCardIds.stream().sorted().toList()) {
            FitMemberCard card = memberCardMapper.selectOne(new LambdaQueryWrapper<FitMemberCard>()
                .eq(FitMemberCard::getCardId, cardId).last("FOR UPDATE"));
            if (card == null) {
                continue;
            }
            if (!Objects.equals(card.getOrderId(), orderId)) {
                throw new ServiceException("订单发卡记录与会员卡来源订单不一致，禁止退款");
            }
            if (nullSafe(card.getUsedTimes()) > 0) {
                throw new ServiceException("会员卡已产生扣次记录，请先完成核销撤销后再退款");
            }
            // A locking read sees usage committed while we waited for the card,
            // including stored/time cards whose usedTimes remains zero.
            List<FitCheckinRecord> successfulCheckins = checkinMapper.selectList(new LambdaQueryWrapper<FitCheckinRecord>()
                .eq(FitCheckinRecord::getCardId, cardId)
                .eq(FitCheckinRecord::getCheckinStatus, "success")
                .last("LIMIT 1 FOR UPDATE"));
            if (!successfulCheckins.isEmpty()) {
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
