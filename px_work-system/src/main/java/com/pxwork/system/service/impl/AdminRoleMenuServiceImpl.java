package com.pxwork.system.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.system.entity.AdminRoleMenu;
import com.pxwork.system.mapper.AdminRoleMenuMapper;
import com.pxwork.system.service.AdminRoleMenuService;

@Service
public class AdminRoleMenuServiceImpl extends ServiceImpl<AdminRoleMenuMapper, AdminRoleMenu> implements AdminRoleMenuService {
}
