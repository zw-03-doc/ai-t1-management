package org.dromara.fitness.config;

import cn.dev33.satoken.stp.StpUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.fitness.domain.model.FitnessLoginUser;
import org.dromara.fitness.service.support.FitnessAccountGuard;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class FitnessAccountWebConfig implements WebMvcConfigurer {
    private final FitnessAccountGuard guard;
    private final RedissonClient locks;
    private static final String LOCK = FitnessAccountWebConfig.class.getName() + ".lock";
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (!StpUtil.isLogin()) return true; // The existing security interceptor handles authentication.
                Object principal = LoginHelper.getLoginUser();
                RLock lock = null;
                if (principal instanceof FitnessLoginUser.MemberLoginUser member && !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) {
                    // Serialize mutations from all devices while a blank WeChat identity is being claimed.
                    lock = locks.getLock("fitness:member-write:" + member.getTenantId() + ":" + member.getMemberId());
                    try { if (!lock.tryLock(5, TimeUnit.SECONDS)) throw new ServiceException("会员信息正在处理中，请稍后重试"); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new ServiceException("请求已中断，请重试"); }
                    request.setAttribute(LOCK, lock);
                }
                try { guard.check(principal); }
                catch (RuntimeException e) { if (lock != null) { lock.unlock(); request.removeAttribute(LOCK); } throw e; }
                return true;
            }
            @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                if (request.getAttribute(LOCK) instanceof RLock lock) lock.unlock();
            }
        }).addPathPatterns("/fitness/**")
          .excludePathPatterns("/fitness/mini/auth/wechatLogin", "/fitness/mini/auth/webLogin", "/fitness/coachApp/auth/login",
              "/fitness/coachApp/auth/wechatLogin", "/fitness/coachApp/auth/wechatActivate")
          .order(10);
    }
}
