package com.pxwork.api.controller.backend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pxwork.system.entity.AdminUser;
import com.pxwork.system.service.AdminUserService;
import com.pxwork.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 后台管理员 前端控制器
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-12
 */
@Tag(name = "后台管理员管理", description = "后台管理员相关的接口")
@RestController
@RequestMapping("/admin-user")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @Operation(summary = "管理员列表(旧接口)", description = "获取所有管理员信息")
    @GetMapping("/test")
    public Result<List<AdminUser>> test() {
        return Result.success(adminUserService.list());
    }

    @Operation(summary = "管理员分页列表", description = "获取管理员分页列表")
    @GetMapping("/list")
    public Result<Page<AdminUser>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        
        Page<AdminUser> page = new Page<>(current, size);
        LambdaQueryWrapper<AdminUser> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like(AdminUser::getName, name);
        }
        queryWrapper.orderByDesc(AdminUser::getCreatedAt);
        
        return Result.success(adminUserService.page(page, queryWrapper));
    }

    @Operation(summary = "新增管理员", description = "创建新管理员(支持分配角色)")
    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AdminUser adminUser) {
        boolean success = adminUserService.createAdminUser(adminUser);
        return success ? Result.success(true) : Result.fail("创建失败");
    }

    @Operation(summary = "修改管理员", description = "更新管理员信息(支持分配角色)")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody AdminUser adminUser) {
        boolean success = adminUserService.updateAdminUser(adminUser);
        return success ? Result.success(true) : Result.fail("更新失败");
    }

    @Operation(summary = "删除管理员", description = "根据ID删除管理员")
    @PostMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = adminUserService.removeById(id);
        return success ? Result.success(true) : Result.fail("删除失败");
    }
}
