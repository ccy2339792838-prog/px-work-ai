package com.pxwork.api.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pxwork.common.utils.StpUserUtil;
import com.pxwork.system.entity.AdminMenu;
import com.pxwork.system.entity.AdminRole;
import com.pxwork.system.entity.AdminRoleMenu;
import com.pxwork.system.entity.AdminUser;
import com.pxwork.system.entity.AdminUserRole;
import com.pxwork.system.service.AdminMenuService;
import com.pxwork.system.service.AdminRoleMenuService;
import com.pxwork.system.service.AdminRoleService;
import com.pxwork.system.service.AdminUserRoleService;
import com.pxwork.system.service.AdminUserService;

import cn.dev33.satoken.stp.StpInterface;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private AdminUserRoleService adminUserRoleService;

    @Autowired
    private AdminRoleMenuService adminRoleMenuService;

    @Autowired
    private AdminMenuService adminMenuService;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private AdminRoleService adminRoleService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (StpUserUtil.TYPE.equals(loginType)) {
            return List.of();
        }
        Long adminId = toLong(loginId);
        if (adminId == null) {
            return List.of();
        }
        AdminUser adminUser = adminUserService.getById(adminId);
        if (adminUser == null) {
            return List.of();
        }
        if (Integer.valueOf(1).equals(adminUser.getIsSuper())) {
            return List.of("*");
        }

        List<AdminUserRole> adminUserRoles = adminUserRoleService.list(new LambdaQueryWrapper<AdminUserRole>()
                .eq(AdminUserRole::getAdminUserId, adminId));
        if (adminUserRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = adminUserRoles.stream().map(AdminUserRole::getRoleId).distinct().collect(Collectors.toList());
        List<AdminRoleMenu> roleMenus = adminRoleMenuService.list(new LambdaQueryWrapper<AdminRoleMenu>()
                .in(AdminRoleMenu::getRoleId, roleIds));
        if (roleMenus.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = roleMenus.stream().map(AdminRoleMenu::getMenuId).distinct().collect(Collectors.toList());
        List<AdminMenu> menus = adminMenuService.list(new LambdaQueryWrapper<AdminMenu>().in(AdminMenu::getId, menuIds));
        if (menus.isEmpty()) {
            return List.of();
        }
        Set<String> perms = new LinkedHashSet<>();
        for (AdminMenu menu : menus) {
            if (StringUtils.hasText(menu.getPerms())) {
                perms.add(menu.getPerms().trim());
            }
        }
        return new ArrayList<>(perms);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (StpUserUtil.TYPE.equals(loginType)) {
            return List.of();
        }
        Long adminId = toLong(loginId);
        if (adminId == null) {
            return List.of();
        }
        List<AdminUserRole> adminUserRoles = adminUserRoleService.list(new LambdaQueryWrapper<AdminUserRole>()
                .eq(AdminUserRole::getAdminUserId, adminId));
        if (adminUserRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = adminUserRoles.stream().map(AdminUserRole::getRoleId).distinct().collect(Collectors.toList());
        List<AdminRole> roles = adminRoleService.listByIds(roleIds);
        if (roles.isEmpty()) {
            return List.of();
        }
        return roles.stream().map(AdminRole::getName).filter(StringUtils::hasText).collect(Collectors.toList());
    }

    private Long toLong(Object loginId) {
        if (loginId == null) {
            return null;
        }
        if (loginId instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(loginId));
        } catch (Exception e) {
            return null;
        }
    }
}
