package com.pxwork.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pxwork.system.entity.AdminUser;

public interface AdminUserService extends IService<AdminUser> {
    boolean createAdminUser(AdminUser adminUser);
    boolean updateAdminUser(AdminUser adminUser);
}
