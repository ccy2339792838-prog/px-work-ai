package com.pxwork.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.system.entity.AdminUserRole;
import com.pxwork.system.mapper.AdminUserRoleMapper;
import com.pxwork.system.service.AdminUserRoleService;
import org.springframework.stereotype.Service;

@Service
public class AdminUserRoleServiceImpl extends ServiceImpl<AdminUserRoleMapper, AdminUserRole> implements AdminUserRoleService {
}
