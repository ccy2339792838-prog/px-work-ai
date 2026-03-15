package com.pxwork.system.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pxwork.system.entity.AdminMenu;

public interface AdminMenuService extends IService<AdminMenu> {

    List<AdminMenu> getMenuTree();
}
