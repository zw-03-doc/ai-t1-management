package org.dromara.fitness.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.domain.dto.FitnessPortalSlogans;
import org.dromara.fitness.mapper.FitnessPortalSloganMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FitnessPortalSloganService {
    private final FitnessPortalSloganMapper mapper;

    public FitnessPortalSlogans publicContent() {
        return read(TenantConstants.DEFAULT_TENANT_ID).withVersion(null);
    }

    public FitnessPortalSlogans adminContent() {
        return read(adminTenant());
    }

    public FitnessPortalSlogans save(FitnessPortalSlogans input) {
        String tenant = adminTenant();
        if (input.version() == 0) {
            try {
                mapper.insert(tenant, input);
            } catch (DuplicateKeyException alreadyCreated) {
                throw conflict();
            }
        } else if (mapper.updateVersion(tenant, input) != 1) {
            throw conflict();
        }
        return input.withVersion(input.version() + 1);
    }

    private FitnessPortalSlogans read(String tenant) {
        FitnessPortalSlogans saved = mapper.find(tenant);
        return saved == null ? FitnessPortalSlogans.defaults() : saved;
    }

    private String adminTenant() {
        if (!TenantHelper.isEnable()) return TenantConstants.DEFAULT_TENANT_ID;
        String tenant = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenant)) throw new ServiceException("无法确定当前门店租户", 403);
        return tenant;
    }

    private ServiceException conflict() {
        return new ServiceException("文案已被其他管理员更新，请重新载入后再保存", 409);
    }
}
