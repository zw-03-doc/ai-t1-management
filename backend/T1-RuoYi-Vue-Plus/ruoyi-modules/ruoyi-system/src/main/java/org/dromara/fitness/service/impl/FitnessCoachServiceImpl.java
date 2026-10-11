package org.dromara.fitness.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.constant.FitnessRoleConstants;
import org.dromara.fitness.domain.FitCoach;
import org.dromara.fitness.domain.FitCoachWechatBind;
import org.dromara.fitness.domain.bo.FitnessCoachBo;
import org.dromara.fitness.domain.vo.FitnessCoachVo;
import org.dromara.fitness.mapper.FitnessCoachMapper;
import org.dromara.fitness.mapper.FitnessCoachWechatBindMapper;
import org.dromara.fitness.service.IFitnessCoachService;
import org.dromara.fitness.utils.FitnessBizCodeUtils;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysConfigService;
import org.dromara.system.service.ISysTenantService;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 教练 Service 实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessCoachServiceImpl implements IFitnessCoachService {

    private static final String DEFAULT_COACH_HIRE_STATUS = "0";
    private static final String INIT_PASSWORD_CONFIG_KEY = "sys.user.initPassword";

    private final FitnessCoachMapper baseMapper;
    private final FitnessCoachWechatBindMapper coachWechatBindMapper;
    private final ISysUserService sysUserService;
    private final ISysConfigService sysConfigService;
    private final ISysTenantService sysTenantService;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public FitnessCoachVo queryById(Long coachId) {
        return baseMapper.selectCoachById(coachId);
    }

    @Override
    public TableDataInfo<FitnessCoachVo> queryPageList(FitnessCoachBo bo, PageQuery pageQuery) {
        return TableDataInfo.build(baseMapper.selectPageCoachList(pageQuery.build(), bo));
    }

    @Override
    public List<FitnessCoachVo> queryList(FitnessCoachBo bo) {
        return baseMapper.selectCoachList(bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(FitnessCoachBo bo) {
        FitCoach add = MapstructUtils.convert(bo, FitCoach.class);
        add.setCoachId(IdGeneratorUtil.nextLongId());
        add.setTenantId(currentTenantId());
        add.setCoachNo(FitnessBizCodeUtils.defaultIfBlank(bo.getCoachNo(), "COA"));
        add.setActivationCode(resolveActivationCode(bo.getActivationCode(), null, add.getCoachId()));
        add.setHireStatus(resolveCoachHireStatus(bo.getHireStatus()));
        validEntityBeforeSave(add);
        syncCoachUserAccount(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            syncCoachWechatBind(add);
            bo.setCoachId(add.getCoachId());
            bo.setCoachNo(add.getCoachNo());
            bo.setActivationCode(add.getActivationCode());
            bo.setUserId(add.getUserId());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(FitnessCoachBo bo) {
        FitCoach db = baseMapper.selectById(bo.getCoachId());
        if (db == null) {
            throw new ServiceException("教练不存在");
        }
        FitCoach update = MapstructUtils.convert(bo, FitCoach.class);
        update.setTenantId(db.getTenantId());
        update.setUserId(db.getUserId());
        update.setCoachNo(FitnessBizCodeUtils.fillOrKeep(bo.getCoachNo(), db.getCoachNo(), "COA"));
        update.setActivationCode(resolveActivationCode(bo.getActivationCode(), db.getActivationCode(), db.getCoachId()));
        update.setHireStatus(resolveCoachHireStatus(StringUtils.defaultIfBlank(bo.getHireStatus(), db.getHireStatus())));

        validEntityBeforeSave(update);
        syncCoachUserAccount(update);
        boolean flag = baseMapper.updateById(update) > 0;
        if (flag) {
            FitCoach latestCoach = baseMapper.selectById(bo.getCoachId());
            syncCoachWechatBind(latestCoach);
            bo.setUserId(latestCoach.getUserId());
            bo.setActivationCode(latestCoach.getActivationCode());
        }

        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            long count = baseMapper.selectCount(new LambdaQueryWrapper<FitCoach>().in(FitCoach::getCoachId, ids));
            if (count != ids.size()) {
                throw new ServiceException("部分数据不存在或已被删除");
            }
        }
        boolean flag = baseMapper.deleteByIds(ids) > 0;
        if (flag) {
            coachWechatBindMapper.delete(new LambdaQueryWrapper<FitCoachWechatBind>().in(FitCoachWechatBind::getCoachId, ids));
        }
        return flag;
    }

    private void validEntityBeforeSave(FitCoach entity) {
        if (StringUtils.isBlank(entity.getMobile())) {
            throw new ServiceException("手机号不能为空");
        }

        if (StringUtils.isNotBlank(entity.getCoachNo()) && baseMapper.exists(new LambdaQueryWrapper<FitCoach>()
            .eq(FitCoach::getCoachNo, entity.getCoachNo())
            .ne(entity.getCoachId() != null, FitCoach::getCoachId, entity.getCoachId()))) {
            throw new ServiceException("教练编号已存在");
        }
        if (StringUtils.isNotBlank(entity.getActivationCode()) && baseMapper.exists(new LambdaQueryWrapper<FitCoach>()
            .eq(FitCoach::getActivationCode, entity.getActivationCode())
            .ne(entity.getCoachId() != null, FitCoach::getCoachId, entity.getCoachId()))) {
            throw new ServiceException("激活码已存在");
        }
        if (entity.getUserId() != null && baseMapper.exists(new LambdaQueryWrapper<FitCoach>()
            .eq(FitCoach::getUserId, entity.getUserId())
            .ne(entity.getCoachId() != null, FitCoach::getCoachId, entity.getCoachId()))) {
            throw new ServiceException("后台账号已绑定其他教练");
        }
        if (StringUtils.isNotBlank(entity.getMobile()) && baseMapper.exists(new LambdaQueryWrapper<FitCoach>()
            .eq(FitCoach::getMobile, entity.getMobile())
            .ne(entity.getCoachId() != null, FitCoach::getCoachId, entity.getCoachId()))) {
            throw new ServiceException("手机号已存在");
        }
    }

    private void syncCoachUserAccount(FitCoach coach) {
        Long roleId = resolveCoachRoleId();
        if (coach.getUserId() == null) {
            coach.setUserId(createCoachUserAccount(coach, roleId));
            return;
        }
        SysUser existUser = sysUserMapper.selectById(coach.getUserId());
        if (existUser == null) {
            coach.setUserId(createCoachUserAccount(coach, roleId));
            return;
        }
        SysUserBo userBo = buildCoachUserBo(coach, roleId);
        userBo.setUserId(existUser.getUserId());
        validateCoachUser(userBo);

        SysUser updateUser = new SysUser();
        updateUser.setUserId(existUser.getUserId());
        updateUser.setUserName(userBo.getUserName());
        updateUser.setNickName(userBo.getNickName());
        updateUser.setUserType(userBo.getUserType());
        updateUser.setPhonenumber(userBo.getPhonenumber());
        updateUser.setSex(userBo.getSex());
        updateUser.setStatus(userBo.getStatus());
        updateUser.setRemark(userBo.getRemark());
        if (sysUserMapper.updateById(updateUser) <= 0) {
            throw new ServiceException("同步教练账号失败");
        }
        sysUserService.insertUserAuth(existUser.getUserId(), new Long[]{roleId});
    }

    private Long createCoachUserAccount(FitCoach coach, Long roleId) {
        ensureTenantAccountBalance();
        SysUserBo userBo = buildCoachUserBo(coach, roleId);
        userBo.setUserId(IdGeneratorUtil.nextLongId());
        String initPassword = sysConfigService.selectConfigByKey(INIT_PASSWORD_CONFIG_KEY);
        if (StringUtils.isBlank(initPassword)) {
            throw new ServiceException("请先配置系统参数 " + INIT_PASSWORD_CONFIG_KEY + " 作为教练初始密码");
        }
        userBo.setPassword(BCrypt.hashpw(initPassword));
        validateCoachUser(userBo);
        int rows = sysUserService.insertUser(userBo);
        if (rows <= 0) {
            throw new ServiceException("创建教练账号失败");
        }
        return userBo.getUserId();
    }

    private void validateCoachUser(SysUserBo userBo) {
        if (!sysUserService.checkUserNameUnique(userBo)) {
            throw new ServiceException("手机号已被占用，无法创建教练账号");
        }
        if (StringUtils.isNotBlank(userBo.getPhonenumber()) && !sysUserService.checkPhoneUnique(userBo)) {
            throw new ServiceException("手机号已存在员工账号，无法创建教练账号");
        }
    }

    private Long resolveCoachRoleId() {
        List<SysRole> roleList = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getRoleKey, FitnessRoleConstants.ROLE_KEY_JIAOLIAN)
            .eq(SysRole::getStatus, SystemConstants.NORMAL));
        if (roleList.isEmpty()) {
            throw new ServiceException("未找到角色标识[" + FitnessRoleConstants.ROLE_KEY_JIAOLIAN + "]对应的教练角色，请先创建角色");
        }
        if (roleList.size() > 1) {
            throw new ServiceException("角色标识[" + FitnessRoleConstants.ROLE_KEY_JIAOLIAN + "]存在重复数据，请先清理后再操作");
        }
        return roleList.get(0).getRoleId();
    }

    private SysUserBo buildCoachUserBo(FitCoach coach, Long roleId) {
        SysUserBo userBo = new SysUserBo();
        userBo.setUserName(coach.getMobile());
        userBo.setNickName(StringUtils.defaultIfBlank(coach.getCoachName(), coach.getMobile()));
        userBo.setUserType(UserType.SYS_USER.getUserType());
        userBo.setPhonenumber(coach.getMobile());
        userBo.setSex(resolveUserSex(coach.getGender()));
        userBo.setStatus(resolveUserStatus(coach.getHireStatus()));
        userBo.setRemark("教练档案自动创建/同步账号");
        userBo.setRoleIds(new Long[]{roleId});
        return userBo;
    }

    private void ensureTenantAccountBalance() {
        if (TenantHelper.isEnable() && !sysTenantService.checkAccountBalance(currentTenantId())) {
            throw new ServiceException("当前租户下用户名额不足，请联系管理员");
        }
    }

    private String resolveCoachHireStatus(String hireStatus) {
        return StringUtils.defaultIfBlank(hireStatus, DEFAULT_COACH_HIRE_STATUS);
    }

    private String resolveActivationCode(String inputCode, String currentCode, Long coachId) {
        if (StringUtils.isNotBlank(inputCode)) {
            return inputCode;
        }
        if (StringUtils.isNotBlank(currentCode)) {
            return currentCode;
        }
        return generateUniqueActivationCode(coachId);
    }

    private String generateUniqueActivationCode(Long coachId) {
        for (int i = 0; i < 20; i++) {
            String code = "A" + ThreadLocalRandom.current().nextInt(100000, 1000000);
            boolean exists = baseMapper.exists(new LambdaQueryWrapper<FitCoach>()
                .eq(FitCoach::getActivationCode, code)
                .ne(coachId != null, FitCoach::getCoachId, coachId));
            if (!exists) {
                return code;
            }
        }
        throw new ServiceException("自动生成激活码失败，请重试");
    }

    private String resolveUserStatus(String hireStatus) {
        return Objects.equals("1", hireStatus) ? "1" : "0";
    }

    private String resolveUserSex(String gender) {
        if (Objects.equals("1", gender)) {
            return "0";
        }
        if (Objects.equals("2", gender)) {
            return "1";
        }
        return "2";
    }

    private void syncCoachWechatBind(FitCoach coach) {
        if (coach == null) {
            return;
        }
        FitCoachWechatBind bind = coachWechatBindMapper.selectOne(new LambdaQueryWrapper<FitCoachWechatBind>()
            .eq(FitCoachWechatBind::getCoachId, coach.getCoachId())
            .last("LIMIT 1"));
        if (bind == null) {
            bind = new FitCoachWechatBind();
            bind.setBindId(IdGeneratorUtil.nextLongId());
            bind.setCoachId(coach.getCoachId());
            bind.setRemark("由教练主表自动同步");
        }
        bind.setTenantId(coach.getTenantId());
        bind.setCoachMobile(coach.getMobile());
        bind.setActivationCode(coach.getActivationCode());
        bind.setBindStatus(resolveBindStatus(coach.getHireStatus()));
        if (bind.getBindId() == null) {
            coachWechatBindMapper.insert(bind);
            return;
        }
        if (coachWechatBindMapper.selectById(bind.getBindId()) == null) {
            coachWechatBindMapper.insert(bind);
        } else {
            coachWechatBindMapper.updateById(bind);
        }
    }

    private String resolveBindStatus(String hireStatus) {
        return Objects.equals("1", hireStatus) ? "1" : "0";
    }

    private String currentTenantId() {
        return StringUtils.defaultIfBlank(TenantHelper.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
    }
}
