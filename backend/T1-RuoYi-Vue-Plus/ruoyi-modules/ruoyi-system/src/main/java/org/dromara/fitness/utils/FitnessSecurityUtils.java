package org.dromara.fitness.utils;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.fitness.constant.FitnessMobileConstants;
import org.dromara.fitness.domain.model.FitnessLoginUser.CoachLoginUser;
import org.dromara.fitness.domain.model.FitnessLoginUser.MemberLoginUser;
import org.dromara.fitness.domain.vo.FitnessCoachAppVo;
import org.dromara.fitness.domain.vo.FitnessMiniVo;

/**
 * 健身房移动端登录态工具
 *
 * @author CodeBuddy
 */
public final class FitnessSecurityUtils {

    private FitnessSecurityUtils() {
    }

    public static void loginMember(FitnessMiniVo.MemberInfoVo memberInfoVo, String openid) {
        loginMember(memberInfoVo, openid, "mini");
    }

    public static void loginMember(FitnessMiniVo.MemberInfoVo memberInfoVo, String openid, String device) {
        MemberLoginUser loginUser = new MemberLoginUser();
        loginUser.setTenantId(memberInfoVo.getTenantId());
        loginUser.setUserType(UserType.APP_USER.getUserType());
        loginUser.setUserId(FitnessMobileConstants.MEMBER_LOGIN_ID_OFFSET + memberInfoVo.getMemberId());
        loginUser.setMemberId(memberInfoVo.getMemberId());
        loginUser.setHomeStoreId(memberInfoVo.getHomeStoreId());
        loginUser.setOpenid(openid);
        loginUser.setUsername(buildMemberUsername(memberInfoVo));
        loginUser.setNickname(memberInfoVo.getDisplayName());
        SaLoginParameter parameter = new SaLoginParameter();
        parameter.setDeviceType(device);
        parameter.setIsShare(false);
        parameter.setTimeout(FitnessMobileConstants.DEFAULT_TIMEOUT);
        parameter.setActiveTimeout(FitnessMobileConstants.DEFAULT_ACTIVE_TIMEOUT);
        parameter.setExtra(LoginHelper.CLIENT_KEY, FitnessMobileConstants.MINI_CLIENT_ID);
        LoginHelper.login(loginUser, parameter);
    }

    public static void loginCoach(FitnessCoachAppVo.CoachProfileVo profileVo) {
        loginCoach(profileVo, "mini");
    }

    public static void loginCoach(FitnessCoachAppVo.CoachProfileVo profileVo, String device) {
        CoachLoginUser loginUser = new CoachLoginUser();
        loginUser.setTenantId(profileVo.getTenantId());
        loginUser.setUserType(UserType.APP_USER.getUserType());
        loginUser.setUserId(profileVo.getUserId());
        loginUser.setCoachId(profileVo.getCoachId());
        loginUser.setStoreId(profileVo.getStoreId());
        loginUser.setUsername(profileVo.getUserName());
        loginUser.setNickname(profileVo.getDisplayName());
        SaLoginParameter parameter = new SaLoginParameter();
        parameter.setTimeout(FitnessMobileConstants.DEFAULT_TIMEOUT);
        parameter.setActiveTimeout(FitnessMobileConstants.DEFAULT_ACTIVE_TIMEOUT);
        parameter.setExtra(LoginHelper.CLIENT_KEY, FitnessMobileConstants.COACH_CLIENT_ID);
        parameter.setDeviceType(device);
        parameter.setIsShare(false);
        LoginHelper.login(loginUser, parameter);
    }

    public static String getCurrentSource() {
        return "web".equals(StpUtil.getLoginDeviceType()) ? "web" : "mini";
    }

    public static MemberLoginUser getMemberLoginUser() {
        Object current = LoginHelper.getLoginUser();
        if (!(current instanceof MemberLoginUser loginUser) || loginUser.getMemberId() == null) {
            throw new ServiceException("当前会员登录状态无效");
        }
        return loginUser;
    }

    public static CoachLoginUser getCoachLoginUser() {
        Object current = LoginHelper.getLoginUser();
        if (!(current instanceof CoachLoginUser loginUser) || loginUser.getCoachId() == null) {
            throw new ServiceException("当前教练登录状态无效");
        }
        return loginUser;
    }

    public static Long getCurrentMemberId() {
        return getMemberLoginUser().getMemberId();
    }

    public static Long getCurrentCoachId() {
        return getCoachLoginUser().getCoachId();
    }

    public static Long getCurrentCoachUserId() {
        return getCoachLoginUser().getUserId();
    }

    public static String getCurrentTenantId() {
        String tenantId = LoginHelper.getTenantId();
        return tenantId == null ? TenantConstants.DEFAULT_TENANT_ID : tenantId;
    }

    public static void logout() {
        StpUtil.logout();
    }

    private static String buildMemberUsername(FitnessMiniVo.MemberInfoVo memberInfoVo) {
        if (memberInfoVo.getMobile() != null && !memberInfoVo.getMobile().isBlank()) {
            return memberInfoVo.getMobile();
        }
        if (memberInfoVo.getNickname() != null && !memberInfoVo.getNickname().isBlank()) {
            return memberInfoVo.getNickname();
        }
        return "member_" + memberInfoVo.getMemberId();
    }
}
