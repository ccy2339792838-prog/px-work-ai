package com.pxwork.api.controller.backend;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pxwork.common.entity.User;
import com.pxwork.common.service.UserService;
import com.pxwork.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学员管理 前端控制器
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
@Tag(name = "学员管理", description = "学员相关的接口")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "学员分页列表", description = "获取学员分页列表(包含部门信息)")
    @GetMapping("/list")
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        
        Page<User> page = new Page<>(current, size);
        return Result.success(userService.pageWithDepts(page, name));
    }

    @Operation(summary = "新增学员", description = "创建新学员(支持分配部门)")
    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody User user) {
        boolean success = userService.createUser(user);
        return success ? Result.success(true) : Result.fail("创建失败");
    }

    @Operation(summary = "修改学员", description = "更新学员信息(支持分配部门)")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody User user) {
        boolean success = userService.updateUser(user);
        return success ? Result.success(true) : Result.fail("更新失败");
    }

    @Operation(summary = "删除学员", description = "根据ID删除学员")
    @PostMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = userService.removeById(id);
        return success ? Result.success(true) : Result.fail("删除失败");
    }
}
