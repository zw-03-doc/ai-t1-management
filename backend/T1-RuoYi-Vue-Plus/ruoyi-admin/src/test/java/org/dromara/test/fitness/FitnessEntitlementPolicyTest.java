package org.dromara.test.fitness;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.*;
import org.dromara.fitness.utils.FitnessEntitlementPolicy;
import org.dromara.fitness.utils.FitnessBookingPolicy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class FitnessEntitlementPolicyTest {
    @Test void totalIncludesBonusExactlyOnce() {
        assertEquals(7, FitnessEntitlementPolicy.grantedTimes(3, 4));
        assertEquals(4, FitnessEntitlementPolicy.grantedTimes(null, 4));
        assertThrows(ServiceException.class, () -> FitnessEntitlementPolicy.grantedTimes(-1, 4));
        assertThrows(ServiceException.class, () -> FitnessEntitlementPolicy.grantedTimes(Integer.MAX_VALUE, 1));
    }
    private final Date now = new Date(1000000);
    private FitMemberCard card() {
        FitMemberCard c = new FitMemberCard(); c.setCardId(1L); c.setTenantId("000000"); c.setMemberId(2L);
        c.setStoreId(3L); c.setCardStatus("active"); c.setPlanType("count_card"); c.setRemainTimes(5); c.setBalanceAmount(new BigDecimal("100"));
        return c;
    }
    private FitCourseSchedule schedule() { FitCourseSchedule s = new FitCourseSchedule(); s.setStoreId(3L); s.setCoachId(4L); s.setStartTime(new Date(2000000)); return s; }
    private FitCourse course() { FitCourse c = new FitCourse(); c.setCourseType("group"); return c; }
    private void check(FitMemberCard c) { FitnessEntitlementPolicy.validate(c, null, course(), schedule(), 2L, "000000", now); }
    @Test void groupCourseAcceptsUsableCountCard() { assertDoesNotThrow(() -> check(card())); }
    @Test void timeCardNeedsNoCountOrBalance() { var c=card(); c.setPlanType("time_card"); c.setRemainTimes(0); c.setBalanceAmount(BigDecimal.ZERO); assertDoesNotThrow(() -> check(c)); }
    @Test void expiryMustCoverClassTime() { var c=card(); c.setExpireTime(new Date(1500000)); assertThrows(ServiceException.class, () -> check(c)); }
    @Test void wrongMemberStoreCoachAndFrozenCardAreRejected() {
        var c=card(); c.setMemberId(9L); assertThrows(ServiceException.class, () -> check(c)); c.setMemberId(2L);
        c.setStoreId(9L); assertThrows(ServiceException.class, () -> check(c)); c.setStoreId(3L);
        c.setBindCoachId(9L); assertThrows(ServiceException.class, () -> check(c)); c.setBindCoachId(null);
        c.setCardStatus("frozen"); assertThrows(ServiceException.class, () -> check(c));
    }
    @Test void courseAndPlanRestrictionsAreEnforced() {
        var p=new FitCardPlan(); p.setSupportCourseType("private,small");
        assertThrows(ServiceException.class, () -> FitnessEntitlementPolicy.validate(card(),p,course(),schedule(),2L,"000000",now));
        var c=course(); c.setSupportCardType("time_card");
        assertThrows(ServiceException.class, () -> FitnessEntitlementPolicy.validate(card(),null,c,schedule(),2L,"000000",now));
    }
    @Test void storedCardRequiresPositiveAmountAndZeroTimes() {
        assertThrows(ServiceException.class, () -> FitnessEntitlementPolicy.consumption(1L,"stored_card",null,null));
        assertThrows(ServiceException.class, () -> FitnessEntitlementPolicy.consumption(1L,"stored_card",1,new BigDecimal("10")));
        var used=FitnessEntitlementPolicy.consumption(1L,"stored_card",null,new BigDecimal("10.25")); assertEquals(0,used.times()); assertEquals(new BigDecimal("10.25"),used.amount());
    }
    @Test void countCardCannotBeRedeemedFreeOrChargedMoney() {
        assertThrows(ServiceException.class, () -> FitnessEntitlementPolicy.consumption(1L,"count_card",0,BigDecimal.ZERO));
        assertThrows(ServiceException.class, () -> FitnessEntitlementPolicy.consumption(1L,"count_card",1,BigDecimal.ONE));
        assertEquals(1,FitnessEntitlementPolicy.consumption(1L,"count_card",null,null).times());
    }
    @Test void timeAndExperienceNeverDebit() {
        assertEquals(0,FitnessEntitlementPolicy.consumption(1L,"time_card",null,null).times());
        assertEquals(0,FitnessEntitlementPolicy.consumption(null,null,null,null).times());
        assertThrows(ServiceException.class, () -> FitnessEntitlementPolicy.consumption(1L,"time_card",1,null));
        assertThrows(ServiceException.class, () -> FitnessEntitlementPolicy.consumption(null,null,0,BigDecimal.ONE));
    }
    @Test void directedAudienceRequiresNamesAndCapacity() {
        assertThrows(ServiceException.class, () -> FitnessBookingPolicy.validateAudience("directed",List.of(),10));
        assertThrows(ServiceException.class, () -> FitnessBookingPolicy.validateAudience("normal",List.of(1L),10));
        assertThrows(ServiceException.class, () -> FitnessBookingPolicy.validateAudience("directed",List.of(1L,2L),1));
        assertDoesNotThrow(() -> FitnessBookingPolicy.validateAudience("directed",List.of(1L,2L),2));
        assertDoesNotThrow(() -> FitnessBookingPolicy.validateAudience("normal",List.of(),1));
    }
}
