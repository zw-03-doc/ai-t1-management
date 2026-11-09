package org.dromara.fitness.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.dromara.fitness.service.support.FitnessPolicyConsent;
import org.dromara.common.core.domain.R;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.fitness.domain.vo.FitnessMiniVo;
import org.dromara.fitness.service.support.FitnessWebLoginService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fitness/mini/auth")
public class FitnessWebLoginController {
    private final FitnessPolicyConsent consent;
    private final FitnessWebLoginService service;
    public record WebCode(@NotBlank @Pattern(regexp = "[a-f0-9]{32}") String code, String consentVersion) { }

    @SaCheckLogin
    @RateLimiter(key = "#{T(org.dromara.common.satoken.utils.LoginHelper).getUserId()}", time = 60, count = 3, message = "登录码生成过于频繁")
    @PostMapping("/webCode")
    public R<Map<String, Object>> generate() { return R.ok(service.generate()); }

    @SaIgnore
    @RateLimiter(limitType = org.dromara.common.ratelimiter.enums.LimitType.IP, time = 60, count = 5, message = "登录尝试过于频繁")
    @PostMapping("/webLogin")
    public R<FitnessMiniVo.LoginVo> login(@Valid @RequestBody WebCode body) {
        consent.requireCurrent(body.consentVersion());
        return R.ok(consent.recordMember(service.login(body.code())));
    }
}
