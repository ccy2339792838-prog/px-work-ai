package com.pxwork.common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pxwork.common.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 学员用户表 服务类
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
public interface UserService extends IService<User> {

    boolean createUser(User user);

    boolean updateUser(User user);
    
    Page<User> pageWithDepts(Page<User> page, String name);
}
