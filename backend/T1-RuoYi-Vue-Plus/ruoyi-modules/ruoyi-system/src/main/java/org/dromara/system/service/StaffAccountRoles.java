package org.dromara.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.bo.SysRoleBo;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Employee administration uses the operator's data scope and grantable menu permissions. */
@Service
@RequiredArgsConstructor
public class StaffAccountRoles {
    private final ISysRoleService roles;
    private final StaffRoleMenus menus;
    private final ISysUserService users;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper memberships;

    public List<SysRoleVo> options() {
        SysRoleBo query = new SysRoleBo();
        query.setStatus(SystemConstants.NORMAL);
        List<SysRoleVo> visible = roles.selectRoleList(query).stream()
            .filter(role -> !role.isSuperAdmin())
            .filter(role -> LoginHelper.isSuperAdmin() || !TenantConstants.TENANT_ADMIN_ROLE_KEY.equals(role.getRoleKey()))
            .toList();
        Set<Long> grantable = menus.assignableRoleIds(visible.stream().map(SysRoleVo::getRoleId).toList());
        return visible.stream().filter(role -> grantable.contains(role.getRoleId())).toList();
    }

    private Set<Long> selectableIds() {
        return options().stream().map(SysRoleVo::getRoleId).collect(Collectors.toSet());
    }

    @Transactional(rollbackFor = Exception.class)
    public int create(SysUserBo user) {
        if (user.getRoleIds() == null || user.getRoleIds().length == 0) throw new ServiceException("请选择账号角色");
        if (!selectableIds().containsAll(Arrays.asList(user.getRoleIds()))) throw new ServiceException("包含不可分配的角色");
        user.setUserId(null);
        return users.insertUser(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public int update(SysUserBo user) {
        if (user.getRoleIds() != null) {
            assign(user.getUserId(), user.getRoleIds(), null);
            user.setRoleIds(null);
        }
        // Password resets require their own permission and hashing route.
        user.setPassword(null);
        return users.updateUser(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assign(Long userId, Long[] requestedIds, Long[] originalRoleIds) {
        if (userId == null) throw new ServiceException("请选择员工账号");
        users.checkUserAllowed(userId);
        users.checkUserDataScope(userId);
        if (userId.equals(LoginHelper.getUserId())) throw new ServiceException("不能修改自己的角色");
        if (requestedIds == null || requestedIds.length == 0) throw new ServiceException("请选择账号角色");
        SysUser employee = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().select(SysUser::getUserId, SysUser::getStatus)
            .eq(SysUser::getUserId, userId).last("FOR UPDATE"));
        if (employee == null) throw new ServiceException("员工账号不存在");

        Set<Long> editable = selectableIds();
        Set<Long> current = memberships.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
            .stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        Set<Long> requested = new HashSet<>(Arrays.asList(requestedIds));
        Set<Long> added = new HashSet<>(requested);
        added.removeAll(current);
        if (!added.isEmpty() && !SystemConstants.NORMAL.equals(employee.getStatus())) throw new ServiceException("停用的账号不能新增角色");
        if (!editable.containsAll(added)) throw new ServiceException("包含不可分配的角色");

        // Memberships outside this operator's scope (including disabled roles) are never removed.
        current.retainAll(editable);
        if (originalRoleIds != null) {
            Set<Long> original = new HashSet<>(Arrays.asList(originalRoleIds));
            original.retainAll(editable);
            if (!original.equals(current)) throw new ServiceException("账号角色已变化，请重新打开后保存");
        }
        current.removeAll(requested);
        if (!current.isEmpty()) {
            memberships.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId).in(SysUserRole::getRoleId, current));
        }
        if (!added.isEmpty()) {
            memberships.insertBatch(added.stream().map(roleId -> {
                SysUserRole entry = new SysUserRole();
                entry.setUserId(userId);
                entry.setRoleId(roleId);
                return entry;
            }).toList());
        }
    }
}
