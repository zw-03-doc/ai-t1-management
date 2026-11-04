package org.dromara.test.fitness;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.domain.FitMember;
import org.dromara.fitness.domain.model.FitnessLoginUser;
import org.dromara.fitness.domain.vo.FitnessMiniVo;
import org.dromara.fitness.mapper.FitnessMemberMapper;
import org.dromara.fitness.mapper.FitnessMiniMapper;
import org.dromara.fitness.service.support.FitnessWebLoginService;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import java.time.Duration;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class FitnessWebCredentialTest {
    private final FitnessMemberMapper members=mock(FitnessMemberMapper.class);
    private final FitnessMiniMapper mini=mock(FitnessMiniMapper.class);
    private final RedissonClient redis=mock(RedissonClient.class);
    private final FitnessWebLoginService service=new FitnessWebLoginService(members,mini,redis);
    private FitMember member() { var m=new FitMember();m.setMemberId(2L);m.setTenantId("000000");m.setMemberStatus("normal");m.setWxOpenid("verified");return m; }

    @Test void newWebCodeReplacesThePriorCodeAndFrozenMembersCannotGenerate() {
        var login=new FitnessLoginUser.MemberLoginUser();login.setMemberId(2L);login.setTenantId("000000");
        RBucket<String> owner=mock(RBucket.class);RBucket<String> credential=mock(RBucket.class);RBucket<Object> old=mock(RBucket.class);
        when(redis.<String>getBucket("fitness:web-login-owner:2")).thenReturn(owner);
        when(redis.<String>getBucket(startsWith("fitness:web-login:"))).thenReturn(credential);
        when(redis.getBucket("fitness:web-login:previous")).thenReturn(old);
        when(owner.get()).thenReturn("previous");var member=member();when(members.selectOne(any())).thenReturn(member);
        try(var auth=mockStatic(FitnessSecurityUtils.class)) {
            auth.when(FitnessSecurityUtils::getMemberLoginUser).thenReturn(login);
            var result=service.generate();assertTrue(result.get("code").toString().matches("[a-f0-9]{32}"));
            verify(old).delete();verify(credential).set("000000:2:v2",Duration.ofSeconds(120));verify(owner).set(result.get("code").toString(),Duration.ofSeconds(120));
            member.setMemberStatus("frozen");assertThrows(ServiceException.class,service::generate);
        }
    }
    @Test void usedAndLegacyCodesNeverLogIn() {
        String code="a".repeat(32);RBucket<String> bucket=mock(RBucket.class);when(redis.<String>getBucket("fitness:web-login:"+code)).thenReturn(bucket);
        when(bucket.getAndDelete()).thenReturn(null,"000000:2");
        assertThrows(ServiceException.class,()->service.login(code));assertThrows(ServiceException.class,()->service.login(code));verifyNoInteractions(members,mini);
    }
    @Test void aCodeForFrozenOrCrossTenantMemberCannotIssueANewSession() {
        String code="a".repeat(32);RBucket<String> bucket=mock(RBucket.class);when(redis.<String>getBucket("fitness:web-login:"+code)).thenReturn(bucket);
        when(bucket.getAndDelete()).thenReturn("000000:2:v2");var member=member();member.setMemberStatus("frozen");when(members.selectOne(any())).thenReturn(member);
        try(var tenant=mockStatic(TenantHelper.class);var auth=mockStatic(FitnessSecurityUtils.class)) {
            tenant.when(()->TenantHelper.dynamic(eq("000000"),any(Supplier.class))).thenAnswer(invocation -> ((Supplier<?>)invocation.getArgument(1)).get());
            assertThrows(ServiceException.class,()->service.login(code));member.setMemberStatus("normal");member.setTenantId("foreign");
            assertThrows(ServiceException.class,()->service.login(code));auth.verifyNoInteractions();
        }
    }
}
