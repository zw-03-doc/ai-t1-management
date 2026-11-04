package org.dromara.test.fitness;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.domain.*;
import org.dromara.fitness.domain.bo.*;
import org.dromara.fitness.domain.model.FitnessLoginUser;
import org.dromara.fitness.mapper.*;
import org.dromara.fitness.service.impl.FitnessCoachServiceImpl;
import org.dromara.fitness.service.impl.FitnessOrderServiceImpl;
import org.dromara.fitness.service.support.*;
import org.dromara.system.domain.*;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.mapper.*;
import org.dromara.system.service.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class FitnessIdentityLifecycleTest {
    @org.junit.jupiter.api.BeforeAll
    static void identifierGenerator() {
        var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        factory.registerSingleton("identifierGenerator", new com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator());
        new org.dromara.common.core.utils.SpringUtils().postProcessBeanFactory(factory);
        // Pure mapper mocks do not initialize entity metadata as a Spring MyBatis context would.
        var configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        var builder = new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "identity-lifecycle-test");
        builder.setCurrentNamespace("identity-lifecycle-test");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(builder, FitOrderItem.class);
    }

    private FitMember member(long id, String mobile, String openid, String source) {
        FitMember m = new FitMember(); m.setMemberId(id); m.setTenantId("000000"); m.setMobile(mobile);
        m.setWxOpenid(openid); m.setRegisterSource(source); m.setMemberStatus("normal"); return m;
    }
    private class IdentityFixture {
        final FitnessIdentityMapper identities = mock(FitnessIdentityMapper.class);
        final FitnessMiniMapper mini = mock(FitnessMiniMapper.class);
        final FitnessIdentityService service = new FitnessIdentityService(identities, mini);
        final FitMember source = member(1L, null, "verified-wechat", "mini");
        final FitMember target = member(2L, "13800000000", null, "admin");
        final FitnessMiniBo.BindPhoneBo bo = new FitnessMiniBo.BindPhoneBo();
        IdentityFixture() {
            bo.setMobile("13800000000");
            when(identities.lockMember("000000", 1L)).thenReturn(source);
            when(identities.lockPhoneOwners("000000", bo.getMobile())).thenReturn(List.of(target));
            when(identities.retireBlankIdentity("000000", 1L)).thenReturn(1);
            when(identities.claimIdentity("000000", 2L, "verified-wechat", null)).thenReturn(1);
        }
        Long bind() { return service.bindVerifiedPhone("000000", 1L, "verified-wechat", bo); }
    }
    @Test void verifiedPhoneClaimsOnlyAnUnusedWechatIdentityAndRevokesItsSessions() {
        var f = new IdentityFixture();
        try (var revoke = mockStatic(FitnessSessionRevoker.class)) {
            assertEquals(2L, f.bind());
            revoke.verify(() -> FitnessSessionRevoker.member(1L));
            verify(f.identities).claimIdentity("000000", 2L, "verified-wechat", null);
            verifyNoInteractions(f.mini);
        }
    }
    @Test void claimingDoesNotMergeAnIdentityThatAlreadyHasBusiness() {
        var f = new IdentityFixture(); when(f.identities.countMemberBusiness("000000",1L)).thenReturn(1L);
        assertThrows(ServiceException.class, f::bind);
        verify(f.identities,never()).retireBlankIdentity(anyString(),anyLong());
    }
    @Test void anAlreadyClaimedOrFrozenTargetIsNeverTransferred() {
        var f = new IdentityFixture(); f.target.setWxOpenid("somebody-else");
        assertThrows(ServiceException.class, f::bind); f.target.setWxOpenid(null); f.target.setMemberStatus("frozen");
        assertThrows(ServiceException.class, f::bind);
        verify(f.identities,never()).claimIdentity(anyString(),anyLong(),anyString(),any());
    }
    @Test void sourceMustMatchTheAuthenticatedWechatIdentity() {
        var f = new IdentityFixture(); f.source.setWxOpenid("different-wechat");
        assertThrows(ServiceException.class, f::bind);
        verify(f.identities,never()).lockPhoneOwners(anyString(),anyString());
    }
    @Test void anotherPreviouslyVerifiedPhoneCannotBeSwappedToClaimSomebodyElse() {
        var f = new IdentityFixture(); f.source.setMobile("13900000000");
        assertThrows(ServiceException.class, f::bind);
        verify(f.identities,never()).retireBlankIdentity(anyString(),anyLong());
    }
    @Test void duplicateLegacyPhoneOwnersRequireManualReview() {
        var f = new IdentityFixture(); when(f.identities.lockPhoneOwners(anyString(),anyString())).thenReturn(List.of(f.target, member(3L,"13800000000",null,"admin")));
        assertThrows(ServiceException.class, f::bind);
    }
    @Test void aPhoneWithoutAnExistingMemberBindsWithoutMovingBusiness() {
        var f = new IdentityFixture(); when(f.identities.lockPhoneOwners(anyString(),anyString())).thenReturn(List.of());
        when(f.mini.updateMemberPhone(1L,f.bo)).thenReturn(1);
        assertEquals(1L,f.bind()); verify(f.identities,never()).countMemberBusiness(anyString(),anyLong());
    }
    @Test void frozenMemberAndCrossTenantSessionAreRejectedAndRevoked() {
        FitnessMemberMapper members=mock(FitnessMemberMapper.class);
        var guard=new FitnessAccountGuard(members,mock(FitnessCoachMapper.class),mock(SysUserMapper.class));
        var login=new FitnessLoginUser.MemberLoginUser(); login.setMemberId(1L);login.setTenantId("000000");
        var m=member(1L,null,"openid","mini");m.setMemberStatus("frozen");when(members.selectById(1L)).thenReturn(m);
        try(var revoke=mockStatic(FitnessSessionRevoker.class)) {
            assertEquals(401,assertThrows(ServiceException.class,()->guard.check(login)).getCode());
            m.setMemberStatus("normal");m.setTenantId("another-tenant");
            assertThrows(ServiceException.class,()->guard.check(login));
            revoke.verify(()->FitnessSessionRevoker.member(1L),times(2));
        }
    }
    @Test void aDisabledCoachCannotUseAnExistingSessionButAnActiveCoachCan() {
        var coaches=mock(FitnessCoachMapper.class);var users=mock(SysUserMapper.class);
        var guard=new FitnessAccountGuard(mock(FitnessMemberMapper.class),coaches,users);
        var login=new FitnessLoginUser.CoachLoginUser();login.setCoachId(4L);login.setUserId(5L);login.setTenantId("000000");
        var c=new FitCoach();c.setCoachId(4L);c.setUserId(5L);c.setTenantId("000000");c.setHireStatus("1");
        var u=new SysUser();u.setUserId(5L);u.setTenantId("000000");u.setStatus("0");
        when(coaches.selectById(4L)).thenReturn(c);when(users.selectById(5L)).thenReturn(u);
        try(var revoke=mockStatic(FitnessSessionRevoker.class)) {
            assertThrows(ServiceException.class,()->guard.check(login));revoke.verify(()->FitnessSessionRevoker.coach(5L));
            c.setHireStatus("0");assertDoesNotThrow(()->guard.check(login));
            u.setStatus("1");assertThrows(ServiceException.class,()->guard.check(login));
        }
    }
    private class OrderFixture {
        final FitnessOrderMapper orders=mock(FitnessOrderMapper.class);
        final FitnessOrderItemMapper items=mock(FitnessOrderItemMapper.class);
        final FitnessMemberCardMapper cards=mock(FitnessMemberCardMapper.class);
        final FitnessCardPlanMapper plans=mock(FitnessCardPlanMapper.class);
        final FitnessMemberMapper members=mock(FitnessMemberMapper.class);
        final FitnessCheckinMapper checkins=mock(FitnessCheckinMapper.class);
        final FitnessCoachMapper coaches=mock(FitnessCoachMapper.class);
        final FitnessStoreMapper stores=mock(FitnessStoreMapper.class);
        final FitnessOrderServiceImpl service=new FitnessOrderServiceImpl(orders,items,cards,plans,members,checkins,coaches,stores);
        final FitOrder order=new FitOrder();final FitCoach coach=new FitCoach();
        OrderFixture() {
            order.setOrderId(10L);order.setTenantId("000000");order.setMemberId(2L);order.setStoreId(3L);order.setBindCoachId(4L);order.setOrderStatus("pending");order.setPayableAmount(new BigDecimal("99.00"));
            when(orders.selectOne(any())).thenReturn(order);when(orders.selectById(10L)).thenReturn(order);when(orders.updateById(any(FitOrder.class))).thenReturn(1);
            FitOrderItem item=new FitOrderItem();item.setItemId(11L);item.setItemType("card_plan");item.setItemRefId(12L);when(items.selectList(any())).thenReturn(List.of(item));
            FitCardPlan plan=new FitCardPlan();plan.setPlanId(12L);plan.setStatus("0");plan.setStoreId(3L);plan.setIsPrivateBindCoach("1");plan.setPlanType("private_package");plan.setTotalTimes(10);when(plans.selectById(12L)).thenReturn(plan);
            when(members.selectOne(any())).thenReturn(member(2L,"13800000000",null,"admin"));
            FitStore store=new FitStore();store.setStoreId(3L);store.setTenantId("000000");store.setStatus("0");when(stores.selectById(3L)).thenReturn(store);
            coach.setCoachId(4L);coach.setStoreId(3L);coach.setTenantId("000000");coach.setHireStatus("0");when(coaches.selectOne(any())).thenReturn(coach);
            when(cards.insert(any(FitMemberCard.class))).thenReturn(1);
        }
    }
    @Test void delayedPaymentIssuesTheCardWithThePersistedCoach() {
        var f=new OrderFixture();var bo=new FitnessOrderBo();bo.setPayMethod("cash");
        try(var tenant=mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("000000");
            assertTrue(f.service.payOrder(10L,bo));
            var card=ArgumentCaptor.forClass(FitMemberCard.class);verify(f.cards).insert(card.capture());
            assertEquals(4L,card.getValue().getBindCoachId());assertEquals(2L,card.getValue().getMemberId());
        }
    }
    @Test void delayedPaymentRejectsAStoppedOrDifferentStoreCoachBeforeIssuance() {
        var f=new OrderFixture();var bo=new FitnessOrderBo();bo.setPayMethod("cash");f.coach.setHireStatus("1");
        try(var tenant=mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("000000");
            assertThrows(ServiceException.class,()->f.service.payOrder(10L,bo));
            f.coach.setHireStatus("0");f.coach.setStoreId(99L);
            assertThrows(ServiceException.class,()->f.service.payOrder(10L,bo));verifyNoInteractions(f.cards);
        }
    }
    @Test void changedAmountRequiresAdministratorToConfirmAgain() {
        var f=new OrderFixture();var bo=new FitnessOrderBo();bo.setPayableAmount(new BigDecimal("98.00"));
        try(var tenant=mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("000000");
            assertThrows(ServiceException.class,()->f.service.payOrder(10L,bo));verifyNoInteractions(f.cards);
            verify(f.orders,never()).updateById(any(FitOrder.class));
        }
    }
    @Test void repeatedPaidOrderDoesNotIssueAnotherCard() {
        var f=new OrderFixture();f.order.setOrderStatus("paid");assertTrue(f.service.payOrder(10L,new FitnessOrderBo()));verifyNoInteractions(f.cards);
    }
    @Test void delayedEditorObservesCommittedPaymentAndCannotChangeIssuedCardBinding() {
        var f = new OrderFixture();
        var payment = new FitnessOrderBo(); payment.setPayMethod("cash");
        when(f.orders.updateById(any(FitOrder.class))).thenAnswer(call -> {
            FitOrder update = call.getArgument(0);
            f.order.setOrderStatus(update.getOrderStatus());
            f.order.setBindCoachId(update.getBindCoachId());
            f.order.setPaidAmount(update.getPaidAmount());
            f.order.setPaidTime(update.getPaidTime());
            return 1;
        });
        try (var tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("000000");
            assertTrue(f.service.payOrder(10L, payment));
            var issuedCard = ArgumentCaptor.forClass(FitMemberCard.class);
            verify(f.cards).insert(issuedCard.capture());

            // A nonlocking snapshot can still contain pending; the locking read sees the committed paid row.
            FitOrder staleSnapshot = new FitOrder();
            staleSnapshot.setOrderId(10L); staleSnapshot.setOrderStatus("pending");
            staleSnapshot.setMemberId(2L); staleSnapshot.setStoreId(3L);
            when(f.orders.selectById(10L)).thenReturn(staleSnapshot);
            clearInvocations(f.coaches, f.items);
            var delayedEdit = new FitnessOrderBo();
            delayedEdit.setOrderId(10L); delayedEdit.setBindCoachId(9L);
            delayedEdit.setDiscountAmount(new BigDecimal("50.00"));

            assertThrows(ServiceException.class, () -> f.service.updateByBo(delayedEdit));
            verify(f.orders, never()).update(any(FitOrder.class), any());
            verifyNoInteractions(f.coaches, f.items);
            assertEquals("paid", f.order.getOrderStatus());
            assertEquals(new BigDecimal("99.00"), f.order.getPaidAmount());
            assertEquals(4L, f.order.getBindCoachId());
            assertEquals(4L, issuedCard.getValue().getBindCoachId());
            verify(f.cards, times(1)).insert(any(FitMemberCard.class));
        }
    }
    @Test void delayedCancellationCannotCancelAnOrderAfterPaymentIssuedItsCard() {
        var f = new OrderFixture();
        when(f.orders.updateById(any(FitOrder.class))).thenAnswer(call -> {
            FitOrder update = call.getArgument(0);
            f.order.setOrderStatus(update.getOrderStatus());
            f.order.setPaidAmount(update.getPaidAmount());
            return 1;
        });
        try (var tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("000000");
            assertTrue(f.service.payOrder(10L, new FitnessOrderBo()));
            var issuedCard = ArgumentCaptor.forClass(FitMemberCard.class);
            verify(f.cards).insert(issuedCard.capture());
            FitOrder staleSnapshot = new FitOrder();
            staleSnapshot.setOrderId(10L); staleSnapshot.setOrderStatus("pending");
            when(f.orders.selectById(10L)).thenReturn(staleSnapshot);

            assertThrows(ServiceException.class, () -> f.service.cancelOrder(10L, "迟到取消"));
            assertEquals("paid", f.order.getOrderStatus());
            assertEquals(new BigDecimal("99.00"), f.order.getPaidAmount());
            assertEquals(4L, issuedCard.getValue().getBindCoachId());
            verify(f.orders, times(1)).updateById(any(FitOrder.class));
            verify(f.cards, times(1)).insert(any(FitMemberCard.class));
        }
    }
    @ParameterizedTest
    @ValueSource(strings = {"times_card", "stored_card", "time_card"})
    void refundSeesUsageCommittedAfterItsOldSnapshotForEveryEntitlement(String planType) {
        var f = new OrderFixture(); f.order.setOrderStatus("paid"); f.order.setPaidAmount(new BigDecimal("99.00"));
        FitMemberCard oldCard = new FitMemberCard();
        oldCard.setCardId(21L); oldCard.setOrderId(10L); oldCard.setPlanType(planType); oldCard.setUsedTimes(0);
        FitMemberCard currentCard = new FitMemberCard();
        currentCard.setCardId(21L); currentCard.setOrderId(10L); currentCard.setPlanType(planType);
        currentCard.setUsedTimes("times_card".equals(planType) ? 1 : 0);
        when(f.cards.selectList(any())).thenReturn(List.of(oldCard));
        when(f.cards.selectById(21L)).thenReturn(oldCard);
        when(f.cards.selectOne(any())).thenReturn(currentCard);
        // The ordinary count belongs to an old RR snapshot; the current locking read sees redemption.
        when(f.checkins.selectCount(any())).thenReturn(0L);
        FitCheckinRecord used = new FitCheckinRecord(); used.setCheckinId(22L); used.setCheckinStatus("success");
        when(f.checkins.selectList(any())).thenReturn(List.of(used));

        assertThrows(ServiceException.class, () -> f.service.refundOrder(10L, new FitnessOrderBo()));
        assertEquals("paid", f.order.getOrderStatus());
        verify(f.cards, never()).deleteById(anyLong());
        verify(f.orders, never()).updateById(any(FitOrder.class));
        verify(f.checkins, never()).selectCount(any());
    }
    @ParameterizedTest
    @ValueSource(strings = {"times_card", "stored_card", "time_card"})
    void unusedEntitlementRefundsOnceAndChecksCurrentUsageBeforeDeleting(String planType) {
        var f = new OrderFixture(); f.order.setOrderStatus("paid"); f.order.setPaidAmount(new BigDecimal("99.00"));
        FitMemberCard card = new FitMemberCard();
        card.setCardId(21L); card.setOrderId(10L); card.setPlanType(planType); card.setUsedTimes(0);
        when(f.cards.selectList(any())).thenReturn(List.of(card));
        when(f.cards.selectOne(any())).thenReturn(card);
        when(f.checkins.selectList(any())).thenReturn(List.of());
        when(f.cards.deleteById(21L)).thenReturn(1);
        when(f.orders.updateById(any(FitOrder.class))).thenAnswer(call -> {
            f.order.setOrderStatus(((FitOrder) call.getArgument(0)).getOrderStatus()); return 1;
        });

        assertTrue(f.service.refundOrder(10L, new FitnessOrderBo()));
        assertEquals("refunded", f.order.getOrderStatus());
        assertTrue(f.service.refundOrder(10L, new FitnessOrderBo()));
        var sequence = inOrder(f.cards, f.checkins);
        sequence.verify(f.cards).selectList(any());
        sequence.verify(f.cards).selectOne(any());
        sequence.verify(f.checkins).selectList(any());
        sequence.verify(f.cards).deleteById(21L);
        verify(f.cards, times(1)).deleteById(21L);
        verify(f.orders, times(1)).updateById(any(FitOrder.class));
    }
    private class CoachFixture {
        final FitnessCoachMapper coaches=mock(FitnessCoachMapper.class);
        final FitnessIdentityMapper identities=mock(FitnessIdentityMapper.class);
        final ISysUserService users=mock(ISysUserService.class);
        final SysUserMapper userMapper=mock(SysUserMapper.class);
        final SysRoleMapper roles=mock(SysRoleMapper.class);
        final FitnessCoachServiceImpl service=new FitnessCoachServiceImpl(coaches,identities,mock(FitnessStoreMapper.class),mock(FitnessCoachWechatBindMapper.class),users,mock(ISysConfigService.class),mock(ISysTenantService.class),roles,userMapper);
        CoachFixture() {
            FitCoach coach=new FitCoach();coach.setCoachId(4L);coach.setUserId(5L);coach.setTenantId("000000");when(coaches.selectList(any())).thenReturn(List.of(coach));when(coaches.deleteByIds(anyCollection())).thenReturn(1);
            SysUser user=new SysUser();user.setUserId(5L);user.setRemark("教练档案自动创建/同步账号");when(userMapper.selectById(5L)).thenReturn(user);
            var role=new SysRoleVo();role.setRoleId(6L);role.setRoleKey("jiaolian");when(roles.selectRolesByUserId(5L)).thenReturn(List.of(role));
        }
    }
    @Test void unusedCoachDeletionRemovesOnlyItsAutomaticAccountAndRevokesAllDevices() {
        var f=new CoachFixture();try(var revoke=mockStatic(FitnessSessionRevoker.class)) {
            assertTrue(f.service.deleteWithValidByIds(List.of(4L),true));verify(f.users).deleteUserByIds(new Long[]{5L});revoke.verify(()->FitnessSessionRevoker.coach(5L));
        }
    }
    @Test void coachWithBusinessCannotBeDeleted() {
        var f=new CoachFixture();when(f.identities.countCoachReferences("000000",4L)).thenReturn(1L);
        assertThrows(ServiceException.class,()->f.service.deleteWithValidByIds(List.of(4L),true));verifyNoInteractions(f.users);
    }
    @Test void independentOrPrivilegedAccountCannotBeDeletedWithTheCoach() {
        var f=new CoachFixture();when(f.identities.countPrivilegedCoachAccount(5L)).thenReturn(1L);
        assertThrows(ServiceException.class,()->f.service.deleteWithValidByIds(List.of(4L),true));verifyNoInteractions(f.users);
        when(f.identities.countPrivilegedCoachAccount(5L)).thenReturn(0L);f.userMapper.selectById(5L).setRemark("手工员工账号");
        assertThrows(ServiceException.class,()->f.service.deleteWithValidByIds(List.of(4L),true));verifyNoInteractions(f.users);
    }
}
