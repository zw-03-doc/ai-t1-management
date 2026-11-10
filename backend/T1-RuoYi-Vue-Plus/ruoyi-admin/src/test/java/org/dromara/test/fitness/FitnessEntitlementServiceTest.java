package org.dromara.test.fitness;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.*;
import org.dromara.fitness.mapper.*;
import org.dromara.fitness.service.support.FitnessEntitlementService;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class FitnessEntitlementServiceTest {
    final FitnessMemberCardMapper cards=mock(FitnessMemberCardMapper.class);
    final FitnessCardPlanMapper plans=mock(FitnessCardPlanMapper.class);
    final FitnessCourseMapper courses=mock(FitnessCourseMapper.class);
    final FitnessScheduleMapper schedules=mock(FitnessScheduleMapper.class);
    final FitnessEntitlementService service=new FitnessEntitlementService(cards,plans,courses,schedules);
    private FitMemberCard setup(String type) {
        var c=new FitMemberCard(); c.setCardId(1L); c.setPlanId(9L); c.setMemberId(2L); c.setStoreId(3L); c.setTenantId("000000"); c.setPlanType(type); c.setCardStatus("active"); c.setRemainTimes(4); c.setUsedTimes(2); c.setBalanceAmount(new BigDecimal("100.00")); c.setVersion(3);
        when(cards.selectOne(any())).thenReturn(c); when(cards.selectById(1L)).thenReturn(c); when(cards.updateById(any(FitMemberCard.class))).thenReturn(1);
        var s=new FitCourseSchedule(); s.setScheduleId(5L); s.setCourseId(6L); s.setStoreId(3L); s.setTenantId("000000"); s.setStartTime(new Date(System.currentTimeMillis()+60000)); when(schedules.selectById(5L)).thenReturn(s);
        var course=new FitCourse(); course.setCourseId(6L); course.setTenantId("000000"); course.setCourseType("group"); when(courses.selectById(6L)).thenReturn(course); return c;
    }
    private FitCourseReservation reservation() { var r=new FitCourseReservation(); r.setCardId(1L); r.setMemberId(2L); r.setScheduleId(5L); return r; }
    @Test void additionalAllowedCourseWorksForEligibilityAndBooking() {
        try (var auth = mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000");
            var card = setup("count_card");
            var plan = new FitCardPlan();
            plan.setPlanId(9L);
            plan.setBindCourseId(99L);
            when(plans.selectById(9L)).thenReturn(plan);
            when(cards.selectList(any())).thenReturn(List.of(card));
            when(plans.hasExtraCourse("000000", 9L, 6L)).thenReturn(true);
            assertEquals(List.of(card), service.eligibleCards(2L, 5L));
            assertEquals(1L, service.reservationRule(2L, 5L, 1L, false).cardId());
            when(plans.hasExtraCourse("000000", 9L, 6L)).thenReturn(false);
            assertTrue(service.eligibleCards(2L, 5L).isEmpty());
            assertThrows(ServiceException.class, () -> service.reservationRule(2L, 5L, 1L, false));
        }
    }
    @Test void noCardBookingRequiresExplicitExperience() {
        assertThrows(ServiceException.class,()->service.reservationRule(2L,5L,null,false));
        assertEquals("store_experience",service.reservationRule(2L,5L,null,true).reservationType());
        assertThrows(ServiceException.class,()->service.reservationRule(2L,5L,1L,true)); verifyNoInteractions(cards);
    }
    @Test void storedRedemptionDeductsMoneyNotTimesAndKeepsVersion() {
        try(MockedStatic<FitnessSecurityUtils> auth=mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000"); setup("stored_card");
            service.consume(reservation(),null,new BigDecimal("25.50"));
            verify(cards).updateById(argThat((FitMemberCard c)->c.getRemainTimes()==4 && c.getUsedTimes()==2 && c.getVersion()==3 && c.getBalanceAmount().equals(new BigDecimal("74.50"))));
        }
    }
    @Test void countRedemptionDeductsOnlyRequestedTimes() {
        try(MockedStatic<FitnessSecurityUtils> auth=mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000"); setup("count_card");
            service.consume(reservation(),2,BigDecimal.ZERO);
            verify(cards).updateById(argThat((FitMemberCard c)->c.getRemainTimes()==2 && c.getUsedTimes()==4 && c.getBalanceAmount().equals(new BigDecimal("100.00"))));
        }
    }
    @Test void insufficientBalanceOrFrozenCardNeverWrites() {
        try(MockedStatic<FitnessSecurityUtils> auth=mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000"); var c=setup("stored_card");
            assertThrows(ServiceException.class,()->service.consume(reservation(),0,new BigDecimal("100.01")));
            c.setCardStatus("frozen"); assertThrows(ServiceException.class,()->service.consume(reservation(),0,BigDecimal.ONE));
            verify(cards,never()).updateById(any(FitMemberCard.class));
        }
    }
    @Test void optimisticWriteFailureIsSurfacedForTransactionRollback() {
        try(MockedStatic<FitnessSecurityUtils> auth=mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000"); setup("count_card"); when(cards.updateById(any(FitMemberCard.class))).thenReturn(0);
            assertThrows(ServiceException.class,()->service.consume(reservation(),1,BigDecimal.ZERO));
        }
    }
    @Test void aCourseBindingLimitsNewBookingsAndKeepsExistingReservationsRedeemable() {
        try (var auth = mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000");
            var card = setup("count_card");
            var plan = new FitCardPlan();
            plan.setBindCourseId(77L);
            when(plans.selectById(9L)).thenReturn(plan);
            when(cards.selectList(any())).thenReturn(List.of(card));
            assertTrue(service.eligibleCards(2L, 5L).isEmpty());
            assertThrows(ServiceException.class, () -> service.reservationRule(2L, 5L, 1L, false));
            assertDoesNotThrow(() -> service.consume(reservation(), 1, BigDecimal.ZERO));
            plan.setBindCourseId(null);
            assertEquals(List.of(card), service.eligibleCards(2L, 5L));
        }
    }

    @Test void eligibleSelectionExcludesWrongStoreAndExhaustedButIncludesTermCard() {
        try(MockedStatic<FitnessSecurityUtils> auth=mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000"); var term=setup("time_card"); term.setRemainTimes(0); term.setBalanceAmount(BigDecimal.ZERO);
            var foreign=setup("stored_card"); foreign.setStoreId(99L);
            var exhausted=setup("count_card"); exhausted.setRemainTimes(0);
            when(cards.selectList(any())).thenReturn(List.of(term,foreign,exhausted));
            assertEquals(List.of(term),service.eligibleCards(2L,5L));
        }
    }
}
