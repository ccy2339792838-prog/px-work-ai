package com.pxwork.common.service;

import com.pxwork.common.entity.AdminUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 后台管理员表 服务类
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-12
 */
public interface AdminUserService extends IService<AdminUser> {

    boolean createAdminUser(AdminUser adminUser);

    boolean updateAdminUser(AdminUser adminUser);
}
