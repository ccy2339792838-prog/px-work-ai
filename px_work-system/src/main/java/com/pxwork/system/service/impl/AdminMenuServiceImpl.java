package com.pxwork.system.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.system.entity.AdminMenu;
import com.pxwork.system.mapper.AdminMenuMapper;
import com.pxwork.system.service.AdminMenuService;

@Service
public class AdminMenuServiceImpl extends ServiceImpl<AdminMenuMapper, AdminMenu> implements AdminMenuService {

    @Override
    public List<AdminMenu> getMenuTree() {
        List<AdminMenu> allMenus = this.list(new LambdaQueryWrapper<AdminMenu>()
                .orderByAsc(AdminMenu::getSort)
                .orderByAsc(AdminMenu::getId));
        return buildChildren(0L, allMenus);
    }

    private List<AdminMenu> buildChildren(Long parentId, List<AdminMenu> allMenus) {
        List<AdminMenu> children = new ArrayList<>();
        for (AdminMenu menu : allMenus) {
            Long menuParentId = menu.getParentId();
            if (!isSameParent(menuParentId, parentId)) {
                continue;
            }
            menu.setChildren(buildChildren(menu.getId(), allMenus));
            children.add(menu);
        }
        return children;
    }

    private boolean isSameParent(Long currentParentId, Long targetParentId) {
        if (currentParentId == null) {
            return targetParentId == null || targetParentId == 0L;
        }
        return currentParentId.equals(targetParentId);
    }
}
