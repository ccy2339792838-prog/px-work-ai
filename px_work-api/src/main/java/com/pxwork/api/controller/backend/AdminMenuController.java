package com.pxwork.api.controller.backend;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pxwork.common.utils.Result;
import com.pxwork.system.entity.AdminMenu;
import com.pxwork.system.service.AdminMenuService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "1.6 后台-菜单权限管理")
@RestController
@RequestMapping("/admin-menu")
public class AdminMenuController {

    @Autowired
    private AdminMenuService adminMenuService;

    @Operation(summary = "权限树", description = "获取完整菜单权限树")
    @GetMapping("/tree")
    public Result<List<AdminMenu>> tree() {
        return Result.success(adminMenuService.getMenuTree());
    }
}
