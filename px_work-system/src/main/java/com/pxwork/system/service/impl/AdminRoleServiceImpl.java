package com.pxwork.system.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.system.entity.AdminRole;
import com.pxwork.system.entity.AdminRoleMenu;
import com.pxwork.system.mapper.AdminRoleMapper;
import com.pxwork.system.mapper.AdminRoleMenuMapper;
import com.pxwork.system.service.AdminRoleService;

@Service
public class AdminRoleServiceImpl extends ServiceImpl<AdminRoleMapper, AdminRole> implements AdminRoleService {

    @Autowired
    private AdminRoleMenuMapper adminRoleMenuMapper;

    @Override
    public Page<AdminRole> pageWithPermissionCount(Page<AdminRole> page, String name) {
        LambdaQueryWrapper<AdminRole> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like(AdminRole::getName, name);
        }
        queryWrapper.orderByDesc(AdminRole::getCreatedAt);
        Page<AdminRole> rolePage = this.page(page, queryWrapper);
        List<AdminRole> records = rolePage.getRecords();
        if (records == null || records.isEmpty()) {
            return rolePage;
        }

        List<Long> roleIds = records.stream().map(AdminRole::getId).collect(Collectors.toList());
        List<AdminRoleMenu> roleMenus = adminRoleMenuMapper.selectList(new LambdaQueryWrapper<AdminRoleMenu>()
                .in(AdminRoleMenu::getRoleId, roleIds));
        Map<Long, Long> countMap = new HashMap<>();
        for (AdminRoleMenu roleMenu : roleMenus) {
            if (roleMenu.getRoleId() == null) {
                continue;
            }
            countMap.put(roleMenu.getRoleId(), countMap.getOrDefault(roleMenu.getRoleId(), 0L) + 1L);
        }
        for (AdminRole role : records) {
            role.setPermissionCount(countMap.getOrDefault(role.getId(), 0L));
        }
        return rolePage;
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        List<AdminRoleMenu> roleMenus = adminRoleMenuMapper.selectList(new LambdaQueryWrapper<AdminRoleMenu>()
                .eq(AdminRoleMenu::getRoleId, roleId));
        return roleMenus.stream().map(AdminRoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignMenus(Long roleId, List<Long> menuIds) {
        adminRoleMenuMapper.delete(new LambdaQueryWrapper<AdminRoleMenu>()
                .eq(AdminRoleMenu::getRoleId, roleId));
        if (menuIds == null || menuIds.isEmpty()) {
            return true;
        }
        List<AdminRoleMenu> roleMenus = new ArrayList<>();
        for (Long menuId : menuIds) {
            if (menuId == null) {
                continue;
            }
            AdminRoleMenu roleMenu = new AdminRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenus.add(roleMenu);
        }
        if (roleMenus.isEmpty()) {
            return true;
        }
        int inserted = 0;
        for (AdminRoleMenu roleMenu : roleMenus) {
            inserted += adminRoleMenuMapper.insert(roleMenu);
        }
        return inserted == roleMenus.size();
    }
}
