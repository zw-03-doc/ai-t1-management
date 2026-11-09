package org.dromara.test.fitness;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.FitCoachWechatBind;
import org.dromara.fitness.domain.bo.FitnessCoachAppBo;
import org.dromara.fitness.domain.dto.FitnessWechatMiniDto;
import org.dromara.fitness.domain.model.FitnessLoginUser;
import org.dromara.fitness.domain.vo.FitnessCoachAppVo;
import org.dromara.fitness.mapper.FitnessCoachAppMapper;
import org.dromara.fitness.mapper.FitnessCoachWechatBindMapper;
import org.dromara.fitness.service.impl.FitnessCoachAppServiceImpl;
import org.dromara.fitness.service.support.FitnessWechatMiniAuthService;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.dromara.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class FitnessCoachWebPasswordTest {
    @Mock FitnessCoachAppMapper accounts;
    @Mock FitnessCoachWechatBindMapper bindings;
    @Mock FitnessWechatMiniAuthService wechat;
    @Mock SysUserMapper employees;
    @InjectMocks FitnessCoachAppServiceImpl service;
    private final FitCoachWechatBind bind = new FitCoachWechatBind();
    private final FitnessCoachAppVo.CoachProfileVo account = new FitnessCoachAppVo.CoachProfileVo();
    private final FitnessLoginUser.CoachLoginUser principal = new FitnessLoginUser.CoachLoginUser();

    @BeforeAll static void initBindingMetadata() {
        var mapper = new MapperBuilderAssistant(new MybatisConfiguration(), "coach-web-password-test");
        mapper.setCurrentNamespace(FitnessCoachWechatBindMapper.class.getName());
        TableInfoHelper.initTableInfo(mapper, FitCoachWechatBind.class);
    }

    @BeforeEach void setUp() {
        bind.setBindId(42L);
        bind.setCoachId(2L);
        bind.setBindStatus("0");
        principal.setCoachId(2L);
        principal.setUserId(3L);
        principal.setTenantId("000000");
        account.setCoachId(2L);
        account.setUserId(3L);
        account.setTenantId("000000");
        account.setHireStatus("0");
        account.setUserStatus("0");
    }

    void exchangeCodeFor(String openid) {
        var proof = new FitnessWechatMiniDto.SessionInfo();
        proof.setOpenid(openid);
        when(wechat.code2Session("fresh-code")).thenReturn(proof);
    }

    @Test void firstActivationSetsACoachOnlyHashAndReturnsTheSameCoach() {
        exchangeCodeFor("verified-openid");
        when(bindings.selectOne(any())).thenReturn(null, bind);
        when(bindings.updateById(bind)).thenReturn(1);
        account.setWebPasswordSet(true);
        when(accounts.selectCoachAuthInfoByCoachId(2L)).thenReturn(account);
        var request = new FitnessCoachAppBo.WechatActivateBo();
        request.setCode("fresh-code");
        request.setOpenid("untrusted-client-value");
        request.setMobile("13900000002");
        request.setActivationCode("activation");
        request.setWebPassword("coach-secret");
        try (var security = mockStatic(FitnessSecurityUtils.class); var tokens = mockStatic(StpUtil.class)) {
            var result = service.wechatActivate(request);
            assertEquals("verified-openid", bind.getWechatOpenid());
            assertTrue(BCrypt.checkpw("coach-secret", bind.getWebPasswordHash()));
            assertNotNull(bind.getActivateTime());
            assertTrue(result.getProfile().isWebPasswordSet());
            security.verify(() -> FitnessSecurityUtils.loginCoach(account, "mini"));
        }
        verifyNoInteractions(employees);
    }

    @Test void retryCannotReplaceAPasswordAlreadyChosenByTheBoundCoach() {
        exchangeCodeFor("verified-openid");
        bind.setWechatOpenid("verified-openid");
        bind.setWebPasswordHash(BCrypt.hashpw("chosen-password"));
        when(bindings.selectOne(any())).thenReturn(bind, bind);
        var request = new FitnessCoachAppBo.WechatActivateBo();
        request.setCode("fresh-code");
        request.setMobile("13900000002");
        request.setActivationCode("activation");
        request.setWebPassword("different-password");
        assertThrows(ServiceException.class, () -> service.wechatActivate(request));
        verify(bindings, never()).updateById(bind);
        verifyNoInteractions(employees);
    }

    @Test void previouslyBoundCoachSetsPasswordWithANewWechatCodeButOnlyForTheMatchingIdentity() {
        bind.setWechatOpenid("my-openid");
        when(bindings.selectOne(any())).thenReturn(bind);
        when(accounts.selectCoachAuthInfoByCoachId(2L)).thenReturn(account);
        var change = new FitnessCoachAppBo.PasswordUpdateBo();
        change.setNewPassword("new-password");
        change.setCode("fresh-code");
        try (var security = mockStatic(FitnessSecurityUtils.class)) {
            security.when(FitnessSecurityUtils::getCoachLoginUser).thenReturn(principal);
            exchangeCodeFor("someone-else");
            assertThrows(ServiceException.class, () -> service.updatePassword(change));
            verify(bindings, never()).updateById(bind);
            exchangeCodeFor("my-openid");
            when(bindings.updateById(bind)).thenReturn(1);
            service.updatePassword(change);
            assertTrue(BCrypt.checkpw(change.getNewPassword(), bind.getWebPasswordHash()));
        }
        verifyNoInteractions(employees);
    }

    @Test void browserChangeRequiresTheCurrentCredentialAndPreservesStaffPassword() {
        account.setPassword(BCrypt.hashpw("historic-staff-password"));
        when(bindings.selectOne(any())).thenReturn(bind);
        when(accounts.selectCoachAuthInfoByCoachId(2L)).thenReturn(account);
        var change = new FitnessCoachAppBo.PasswordUpdateBo();
        change.setNewPassword("new-password");
        try (var security = mockStatic(FitnessSecurityUtils.class)) {
            security.when(FitnessSecurityUtils::getCoachLoginUser).thenReturn(principal);
            assertThrows(ServiceException.class, () -> service.updatePassword(change));
            change.setOldPassword("invalid-password");
            assertThrows(ServiceException.class, () -> service.updatePassword(change));
            verify(bindings, never()).updateById(bind);
            change.setOldPassword("historic-staff-password");
            when(bindings.updateById(bind)).thenReturn(1);
            service.updatePassword(change);
            assertTrue(BCrypt.checkpw("new-password", bind.getWebPasswordHash()));
        }
        verifyNoInteractions(employees, wechat);
    }
}
