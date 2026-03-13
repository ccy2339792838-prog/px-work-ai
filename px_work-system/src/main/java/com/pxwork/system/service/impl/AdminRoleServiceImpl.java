package com.pxwork.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.system.entity.AdminRole;
import com.pxwork.system.mapper.AdminRoleMapper;
import com.pxwork.system.service.AdminRoleService;
import org.springframework.stereotype.Service;

@Service
public class AdminRoleServiceImpl extends ServiceImpl<AdminRoleMapper, AdminRole> implements AdminRoleService {
}
