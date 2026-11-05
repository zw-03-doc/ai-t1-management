package org.dromara.fitness.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.fitness.domain.dto.FitnessPortalSlogans;
import org.dromara.fitness.service.impl.FitnessPortalSloganService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fitness")
public class FitnessPortalSloganController {
    private final FitnessPortalSloganService service;

    @SaIgnore
    @GetMapping("/mini/portal/slogans")
    public R<FitnessPortalSlogans> publicContent(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return R.ok(service.publicContent());
    }

    @SaCheckPermission("fitness:portal:query")
    @GetMapping("/portal/slogans")
    public R<FitnessPortalSlogans> adminContent(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return R.ok(service.adminContent());
    }

    @Log(title = "首页励志标语", businessType = BusinessType.UPDATE)
    @SaCheckPermission("fitness:portal:edit")
    @PutMapping("/portal/slogans")
    public R<FitnessPortalSlogans> save(@Valid @RequestBody FitnessPortalSlogans input) {
        return R.ok(service.save(input));
    }
}
