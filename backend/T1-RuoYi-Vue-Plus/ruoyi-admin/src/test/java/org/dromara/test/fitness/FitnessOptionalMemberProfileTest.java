package org.dromara.test.fitness;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.fitness.domain.bo.FitnessMiniBo.ProfileUpdateBo;
import org.dromara.fitness.domain.bo.FitnessMiniBo.WechatLoginBo;
import org.dromara.fitness.domain.dto.FitnessWechatMiniDto.SessionInfo;
import org.dromara.fitness.domain.vo.FitnessMiniVo.MemberInfoVo;
import org.dromara.fitness.mapper.FitnessMiniMapper;
import org.dromara.fitness.service.impl.FitnessMiniServiceImpl;
import org.dromara.fitness.service.support.FitnessMemberAvatarService;
import org.dromara.fitness.service.support.FitnessWechatMiniAuthService;
import org.dromara.fitness.utils.FitnessSecurityUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class FitnessOptionalMemberProfileTest {
    private final FitnessMiniMapper mini = mock(FitnessMiniMapper.class);
    private final FitnessWechatMiniAuthService wechat = mock(FitnessWechatMiniAuthService.class);
    private final FitnessMemberAvatarService avatars = mock(FitnessMemberAvatarService.class);
    private final FitnessMiniServiceImpl service = new FitnessMiniServiceImpl(null, null, null, mini, wechat, avatars);

    @BeforeAll
    static void initializeIdentifiers() {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("identifierGenerator", new DefaultIdentifierGenerator());
        new SpringUtils().postProcessBeanFactory(beans);
    }

    @Test
    void legacyClientSuppliedIdentityAndProfileCannotReplaceTheServerIdentityOrStoredNickname() throws Exception {
        var input = new ObjectMapper().readValue("""
            {"code":"temporary-code","openid":"spoofed","mobile":"13800000000","nickname":"overwrite",
             "avatarUrl":"https://invalid.example/user.jpg","homeStoreId":123}
            """, WechatLoginBo.class);
        var verified = new SessionInfo();
        verified.setOpenid("verified-openid");
        var member = new MemberInfoVo();
        member.setMemberId(7L);
        member.setMemberStatus("normal");
        member.setNickname("用户自填");
        member.setProfileSetupStatus("skipped");
        when(wechat.code2Session("temporary-code")).thenReturn(verified);
        when(mini.selectMemberByOpenid("000000", "verified-openid")).thenReturn(member);
        try (var security = mockStatic(FitnessSecurityUtils.class); var token = mockStatic(StpUtil.class)) {
            var result = service.wechatLogin(input);
            assertEquals("verified-openid", result.getOpenid());
            assertEquals("用户自填", result.getMemberInfo().getNickname());
            assertEquals("skipped", result.getMemberInfo().getProfileSetupStatus());
            security.verify(() -> FitnessSecurityUtils.loginMember(member, "verified-openid"));
            verify(mini).upsertWechatIdentity(anyLong(), anyString(), eq("000000"), eq("verified-openid"), isNull());
            verify(mini).selectMemberByOpenid("000000", "verified-openid");
            verifyNoMoreInteractions(mini);
        }
    }

    @Test
    void skipOnlyAcknowledgesTheCurrentMembersPromptEvenIfAnOldFormSendsOtherValues() {
        var input = new ProfileUpdateBo();
        input.setProfileSetupAction("skip");
        input.setNickname("忽略这次编辑");
        input.setContactMobile("13800000000");
        try (var security = mockStatic(FitnessSecurityUtils.class)) {
            security.when(FitnessSecurityUtils::getCurrentMemberId).thenReturn(7L);
            service.updateProfile(input);
        }
        verify(mini).skipPendingProfileSetup(7L);
        verifyNoMoreInteractions(mini);
        verifyNoInteractions(avatars);
    }

    @Test
    void avatarValidationFailureDoesNotSaveTextOrCompleteOnboarding() {
        var member = new MemberInfoVo();
        member.setMemberStatus("normal");
        var input = new ProfileUpdateBo();
        input.setAvatarUrl("/fitness/member/avatars/9/foreign.jpg");
        input.setProfileSetupAction("complete");
        when(mini.selectMemberById(7L)).thenReturn(member);
        doThrow(new ServiceException("头像不属于当前会员")).when(avatars).validateSelection(7L, null, input.getAvatarUrl());
        try (var security = mockStatic(FitnessSecurityUtils.class)) {
            security.when(FitnessSecurityUtils::getCurrentMemberId).thenReturn(7L);
            assertThrows(ServiceException.class, () -> service.updateProfile(input));
        }
        verify(mini, never()).updateMemberProfile(anyLong(), any());
    }

    @Test
    void allProfileFieldsAreOptionalAndInvalidPhoneOrUnknownActionIsRejected() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var input = new ProfileUpdateBo();
            input.setProfileSetupAction("complete");
            assertTrue(validator.validate(input).isEmpty());
            for (String phone : new String[]{"", "13800000000", "+447700900123"}) {
                input.setContactMobile(phone);
                assertTrue(validator.validate(input).isEmpty());
            }
            input.setContactMobile("not-a-number");
            assertFalse(validator.validate(input).isEmpty());
            input.setContactMobile(null);
            input.setProfileSetupAction("pending");
            assertFalse(validator.validate(input).isEmpty());
        }
    }
}
