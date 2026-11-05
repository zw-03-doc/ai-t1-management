package org.dromara.test.fitness;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.controller.FitnessPortalSloganController;
import org.dromara.fitness.domain.dto.FitnessPortalSlogans;
import org.dromara.fitness.mapper.FitnessPortalSloganMapper;
import org.dromara.fitness.service.impl.FitnessPortalSloganService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class FitnessPortalSloganTest {
    private final FitnessPortalSloganMapper mapper = mock(FitnessPortalSloganMapper.class);
    private final FitnessPortalSloganService service = new FitnessPortalSloganService(mapper);
    private final FitnessPortalSlogans defaults = FitnessPortalSlogans.defaults();

    @Test
    void publicPortalShowsDefaultsThenSavedCopyWithoutTenantOrVersionAndWithoutCaching() {
        when(mapper.find(TenantConstants.DEFAULT_TENANT_ID)).thenReturn(null, edited(3));
        try (var tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("another-tenant");
            var controller = new FitnessPortalSloganController(service);
            var response = new MockHttpServletResponse();
            assertEquals(defaults.withVersion(null), controller.publicContent(response).getData());
            assertEquals("no-store", response.getHeader("Cache-Control"));
            var json = new ObjectMapper().valueToTree(controller.publicContent(response).getData());
            assertEquals(7, json.size());
            assertEquals("坚持训练", json.get("subtitle").asText());
            assertFalse(json.has("version"));
            assertFalse(json.has("tenantId"));
            verify(mapper, times(2)).find(TenantConstants.DEFAULT_TENANT_ID);
            verifyNoMoreInteractions(mapper);
        }
    }

    @Test
    void adminsReadAndSaveOnlyTheirCurrentTenantAndConflictingCreatesOrEditsFail() {
        try (var tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::isEnable).thenReturn(true);
            tenant.when(TenantHelper::getTenantId).thenReturn("store-company");
            assertEquals(defaults, service.adminContent());
            verify(mapper).find("store-company");

            var first = edited(0);
            when(mapper.insert("store-company", first)).thenReturn(1).thenThrow(new DuplicateKeyException("duplicate"));
            assertEquals(edited(1), service.save(first));
            assertEquals(409, assertThrows(ServiceException.class, () -> service.save(first)).getCode());

            var current = edited(3);
            when(mapper.updateVersion("store-company", current)).thenReturn(1, 0);
            assertEquals(edited(4), service.save(current));
            assertEquals(409, assertThrows(ServiceException.class, () -> service.save(current)).getCode());

            tenant.when(TenantHelper::getTenantId).thenReturn(null);
            assertEquals(403, assertThrows(ServiceException.class, service::adminContent).getCode());
            assertEquals(403, assertThrows(ServiceException.class, () -> service.save(current)).getCode());
            verify(mapper, never()).find(TenantConstants.DEFAULT_TENANT_ID);
        }
    }

    @Test
    void optionalAccentAndEveryDisplayLimitAreValidatedBeforeSaving() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var valid = edited(0);
            assertEquals("", valid.titleAccent());
            assertTrue(validator.validate(valid).isEmpty());
            var invalid = new FitnessPortalSlogans("x".repeat(33), "x".repeat(13), "x".repeat(9),
                "x".repeat(17), "x".repeat(61), "x".repeat(41), "x".repeat(41), -1);
            assertEquals(8, validator.validate(invalid).size());
            var missing = new FitnessPortalSlogans(null, "", null, " ", "", " ", "", null);
            assertEquals(8, validator.validate(missing).size());
        }
    }

    private FitnessPortalSlogans edited(int version) {
        return new FitnessPortalSlogans(defaults.eyebrow(), defaults.titleLead(), " ", defaults.titleSecondLine(),
            " 坚持训练 ", defaults.galleryCaption(), defaults.footerCaption(), version);
    }
}
