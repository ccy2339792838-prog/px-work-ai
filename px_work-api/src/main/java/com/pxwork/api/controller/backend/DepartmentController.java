package com.pxwork.api.controller.backend;

import com.pxwork.common.entity.Department;
import com.pxwork.common.service.DepartmentService;
import com.pxwork.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 部门管理 前端控制器
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
@Tag(name = "部门管理", description = "部门相关的接口")
@RestController
@RequestMapping("/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Operation(summary = "部门树形列表", description = "获取部门树形结构")
    @GetMapping("/tree")
    public Result<List<Department>> tree() {
        return Result.success(departmentService.getTree());
    }

    @Operation(summary = "新增部门", description = "创建新部门")
    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody Department department) {
        boolean success = departmentService.save(department);
        return success ? Result.success(true) : Result.fail("创建失败");
    }

    @Operation(summary = "修改部门", description = "更新部门信息")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody Department department) {
        boolean success = departmentService.updateById(department);
        return success ? Result.success(true) : Result.fail("更新失败");
    }

    @Operation(summary = "删除部门", description = "根据ID删除部门")
    @PostMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = departmentService.removeById(id);
        return success ? Result.success(true) : Result.fail("删除失败");
    }
}
