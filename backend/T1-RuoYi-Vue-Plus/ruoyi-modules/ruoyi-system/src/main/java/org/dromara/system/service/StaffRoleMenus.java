package org.dromara.system.service;

import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysMenu;
import org.dromara.system.domain.SysRoleMenu;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysMenuVo;
import org.dromara.system.mapper.SysMenuMapper;
import org.dromara.system.mapper.SysRoleMenuMapper;
import org.dromara.system.mapper.SysRoleMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffRoleMenus {

    private final ISysMenuService menuService;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleMapper roleMapper;

    private static final Map<String, String> STAFF_PAGES = Map.of(
        "system/user/index", "员工账号", "system/role/index", "角色权限",
        "system/user/authRole", "设置员工角色", "system/role/authUser", "角色成员");
    private static final Set<String> STAFF_ACTIONS = Set.of(
        "system:user:query", "system:user:add", "system:user:edit", "system:user:remove", "system:user:resetPwd",
        "system:role:query", "system:role:add", "system:role:edit", "system:role:remove");

    public record Options(List<Tree<Long>> menus, Set<Long> checkedKeys) {}

    /** Include only project actions reachable through the operator's active grants. */
    private Map<Long, SysMenuVo> editableMenus() {
        Map<Long, SysMenuVo> active = new LinkedHashMap<>();
        for (SysMenuVo menu : menuService.selectMenuList(LoginHelper.getUserId())) {
            if ("0".equals(menu.getStatus())) active.put(menu.getMenuId(), menu);
        }
        Map<Long, SysMenuVo> allowed = new LinkedHashMap<>();
        for (SysMenuVo menu : active.values()) {
            String component = Objects.toString(menu.getComponent(), "");
            boolean projectPage = component.startsWith("fitness/") || STAFF_PAGES.containsKey(component);
            String permission = Objects.toString(menu.getPerms(), "");
            SysMenuVo parent = active.get(menu.getParentId());
            boolean action = "F".equals(menu.getMenuType()) && parent != null &&
                ((Objects.toString(parent.getComponent(), "").startsWith("fitness/") && permission.startsWith("fitness:"))
                || (STAFF_PAGES.containsKey(Objects.toString(parent.getComponent(), "")) && STAFF_ACTIONS.contains(permission)));
            if (!projectPage && !action) continue;
            Map<Long, SysMenuVo> branch = new LinkedHashMap<>();
            SysMenuVo node = menu;
            while (node != null && !branch.containsKey(node.getMenuId())) {
                branch.put(node.getMenuId(), node);
                if (Long.valueOf(0).equals(node.getParentId())) {
                    allowed.putAll(branch);
                    break;
                }
                node = active.get(node.getParentId());
            }
            if (STAFF_PAGES.containsKey(component)) {
                menu.setMenuName(STAFF_PAGES.get(component));
                if (parent != null) parent.setMenuName("员工与权限");
            } else if (STAFF_ACTIONS.contains(permission)) {
                menu.setMenuName(menu.getMenuName().replace("用户", "员工"));
            }
        }
        return allowed;
    }

    private Set<Long> grants(Long roleId) {
        if (roleId == null) return new HashSet<>();
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
            .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
    }

    public Set<Long> assignableRoleIds(Collection<Long> roleIds) {
        Set<Long> assignable = new HashSet<>(roleIds);
        if (assignable.isEmpty() || LoginHelper.isSuperAdmin()) return assignable;
        List<SysRoleVo> ownRoles = roleMapper.selectRolesByUserId(LoginHelper.getUserId()).stream()
            .filter(role -> "0".equals(role.getStatus())).toList();
        if (ownRoles.stream().noneMatch(role -> "1".equals(role.getDataScope()))) {
            // Custom scopes belong to the role. Relative scopes also depend on the assignee's department.
            Set<Long> ownedCustom = ownRoles.stream().filter(role -> "2".equals(role.getDataScope()))
                .map(SysRoleVo::getRoleId).collect(Collectors.toSet());
            Set<String> ownScopes = ownRoles.stream().map(SysRoleVo::getDataScope).collect(Collectors.toSet());
            boolean departmentOnly = !ownScopes.isEmpty() && Set.of("3", "4").containsAll(ownScopes);
            Set<Long> withinScope = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                    .select(SysRole::getRoleId, SysRole::getDataScope).in(SysRole::getRoleId, assignable)).stream()
                .filter(role -> "5".equals(role.getDataScope()) || ownedCustom.contains(role.getRoleId())
                    || (departmentOnly && ("3".equals(role.getDataScope())
                    || ("4".equals(role.getDataScope()) && ownScopes.contains("4")))))
                .map(SysRole::getRoleId).collect(Collectors.toSet());
            assignable.retainAll(withinScope);
        }
        Set<Long> owned = menuService.selectMenuList(LoginHelper.getUserId()).stream()
            .map(SysMenuVo::getMenuId).collect(Collectors.toSet());
        for (SysRoleMenu grant : roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                .in(SysRoleMenu::getRoleId, roleIds))) {
            if (!owned.contains(grant.getMenuId())) assignable.remove(grant.getRoleId());
        }
        return assignable;
    }

    public void checkAssignable(Long roleId) {
        if (!assignableRoleIds(List.of(roleId)).contains(roleId)) {
            throw new ServiceException("该角色包含超出本人授权范围的权限");
        }
    }

    public Options options(Long roleId) {
        Map<Long, SysMenuVo> allowed = editableMenus();
        Set<Long> checked = grants(roleId);
        checked.retainAll(allowed.keySet());
        // TreeBuildUtils takes the root parent ID from the first entry.
        List<SysMenuVo> ordered = allowed.values().stream().sorted(Comparator.comparing(SysMenuVo::getParentId)
            .thenComparing(SysMenuVo::getOrderNum)).toList();
        return new Options(menuService.buildMenuTreeSelect(ordered), checked);
    }

    /** Called within the role write transaction. Only change grants actually edited. */
    public void save(Long roleId, Long[] selectedIds, Long[] originalIds) {
        if (selectedIds == null || originalIds == null) throw new ServiceException("请重新打开角色权限后保存");
        Map<Long, SysMenuVo> allowed = editableMenus();
        Set<Long> selected = new HashSet<>(Arrays.asList(selectedIds));
        if (!allowed.keySet().containsAll(selected)) throw new ServiceException("包含不可分配的权限");
        Set<Long> current = grants(roleId);
        Set<Long> displayed = new HashSet<>(current);
        displayed.retainAll(allowed.keySet());
        if (!displayed.equals(new HashSet<>(Arrays.asList(originalIds)))) {
            throw new ServiceException("角色权限已变化，请重新打开后保存");
        }

        Set<Long> added = new HashSet<>(selected);
        added.removeAll(current);
        for (Long id : List.copyOf(added)) {
            SysMenuVo node = allowed.get(id);
            while (allowed.containsKey(node.getParentId())) {
                node = allowed.get(node.getParentId());
                if (!current.contains(node.getMenuId())) added.add(node.getMenuId());
            }
        }
        Set<Long> removed = new HashSet<>(displayed);
        removed.removeAll(selected);
        if (!removed.isEmpty()) {
            // Hidden children can depend on visible directories or pages.
            Map<Long, Long> parents = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .select(SysMenu::getMenuId, SysMenu::getParentId)).stream()
                .filter(menu -> menu.getParentId() != null)
                .collect(Collectors.toMap(SysMenu::getMenuId, SysMenu::getParentId));
            Set<Long> kept = new HashSet<>(current);
            kept.removeAll(removed);
            kept.addAll(added);
            Set<Long> visited = new HashSet<>();
            for (Long id : kept) {
                Long parent = parents.get(id);
                while (parent != null && visited.add(parent)) {
                    removed.remove(parent);
                    parent = parents.get(parent);
                }
            }
            if (!removed.isEmpty()) roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId).in(SysRoleMenu::getMenuId, removed));
        }
        if (!added.isEmpty()) {
            roleMenuMapper.insertBatch(added.stream().map(id -> {
                SysRoleMenu grant = new SysRoleMenu();
                grant.setRoleId(roleId);
                grant.setMenuId(id);
                return grant;
            }).toList());
        }
    }
}
