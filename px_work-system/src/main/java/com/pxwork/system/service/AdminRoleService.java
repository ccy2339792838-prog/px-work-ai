package com.pxwork.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pxwork.system.entity.AdminRole;

import java.util.List;

public interface AdminRoleService extends IService<AdminRole> {

    Page<AdminRole> pageWithPermissionCount(Page<AdminRole> page, String name);

    List<Long> getRoleMenuIds(Long roleId);

    boolean assignMenus(Long roleId, List<Long> menuIds);
}
