package com.pxwork.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.common.entity.AdminUser;
import com.pxwork.common.entity.AdminUserRole;
import com.pxwork.common.mapper.AdminUserMapper;
import com.pxwork.common.service.AdminUserRoleService;
import com.pxwork.common.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 后台管理员表 服务实现类
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-12
 */
@Service
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements AdminUserService {

    @Autowired
    private AdminUserRoleService adminUserRoleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createAdminUser(AdminUser adminUser) {
        // 1. 保存管理员基本信息
        boolean saved = this.save(adminUser);
        if (!saved) {
            return false;
        }

        // 2. 保存角色关联
        saveRoles(adminUser.getId(), adminUser.getRoleIds());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAdminUser(AdminUser adminUser) {
        // 1. 更新管理员基本信息
        boolean updated = this.updateById(adminUser);
        if (!updated) {
            return false;
        }

        // 2. 更新角色关联（如果 roleIds 不为 null）
        if (adminUser.getRoleIds() != null) {
            // 先删除旧关联
            adminUserRoleService.remove(new LambdaQueryWrapper<AdminUserRole>()
                    .eq(AdminUserRole::getAdminUserId, adminUser.getId()));
            // 再保存新关联
            saveRoles(adminUser.getId(), adminUser.getRoleIds());
        }
        return true;
    }

    private void saveRoles(Long adminUserId, List<Long> roleIds) {
        if (roleIds != null && !roleIds.isEmpty()) {
            List<AdminUserRole> userRoles = roleIds.stream().map(roleId -> {
                AdminUserRole userRole = new AdminUserRole();
                userRole.setAdminUserId(adminUserId);
                userRole.setRoleId(roleId);
                return userRole;
            }).collect(Collectors.toList());
            adminUserRoleService.saveBatch(userRoles);
        }
    }
}
