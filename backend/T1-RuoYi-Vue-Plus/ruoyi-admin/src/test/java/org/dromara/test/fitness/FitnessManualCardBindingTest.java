package org.dromara.test.fitness;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.*;
import org.dromara.fitness.domain.bo.FitnessCardBindBo;
import org.dromara.fitness.mapper.*;
import org.dromara.fitness.service.impl.FitnessMemberCardServiceImpl;
import org.dromara.fitness.service.support.FitnessCardIssuer;
import org.dromara.fitness.service.support.FitnessOperationsService;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class FitnessManualCardBindingTest {
    final FitnessMemberCardMapper cards = mock(FitnessMemberCardMapper.class);
    final FitnessMemberMapper members = mock(FitnessMemberMapper.class);
    final FitnessCardPlanMapper plans = mock(FitnessCardPlanMapper.class);
    final FitnessCardIssuer issuer = mock(FitnessCardIssuer.class);
    final FitnessOperationsService audit = mock(FitnessOperationsService.class);
    final FitnessMemberCardServiceImpl service = new FitnessMemberCardServiceImpl(cards, members, plans, issuer, audit);
    final FitnessCardBindBo request = new FitnessCardBindBo(1L, 2L, 3L, null, UUID.randomUUID(), "");

    FitnessManualCardBindingTest() {
        var member = new FitMember();
        member.setMemberStatus("normal");
        when(members.selectOne(any())).thenReturn(member);
    }

    @Test void replayReturnsTheOriginalCardEvenAfterItsRightsWereSpent() {
        var card = new FitMemberCard();
        card.setMemberId(1L);
        card.setPlanId(2L);
        card.setStoreId(3L);
        card.setCardId(4L);
        card.setCardNo("MANUAL-" + request.requestId());
        card.setCardStatus("exhausted");
        when(cards.selectOne(any())).thenReturn(card);
        try (var security = mockStatic(FitnessSecurityUtils.class)) {
            security.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000");
            var result = service.bind(request);
            assertEquals(4L, result.cardId());
            assertFalse(result.created());
            verifyNoInteractions(issuer, audit);
        }
    }

    @Test void bindingAPrivatePackageDoesNotAddTheMemberToItsRoster() {
        var plan = new FitCardPlan();
        plan.setStatus("0");
        plan.setPlanType("private_package");
        plan.setCustomMemberIds("11,12");
        when(plans.selectOne(any())).thenReturn(plan);
        try (var security = mockStatic(FitnessSecurityUtils.class)) {
            security.when(FitnessSecurityUtils::getCurrentTenantId).thenReturn("000000");
            assertThrows(ServiceException.class, () -> service.bind(request));
            verifyNoInteractions(issuer, audit);
        }
    }
}
