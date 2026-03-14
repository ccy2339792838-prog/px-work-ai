package com.pxwork.api.controller.backend;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pxwork.common.entity.Department;
import com.pxwork.common.entity.UserDepartment;
import com.pxwork.common.service.DepartmentService;
import com.pxwork.common.service.UserDepartmentService;
import com.pxwork.common.utils.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
    @Autowired
    private UserDepartmentService userDepartmentService;

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
    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody Department department) {
        boolean success = departmentService.updateById(department);
        return success ? Result.success(true) : Result.fail("更新失败");
    }

    @Operation(summary = "删除部门", description = "根据ID删除部门")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        long children = departmentService.count(new LambdaQueryWrapper<Department>().eq(Department::getParentId, id));
        if (children > 0) {
            return Result.fail("请先删除子部门");
        }
        long userRefs = userDepartmentService.count(new LambdaQueryWrapper<UserDepartment>().eq(UserDepartment::getDepartmentId, id));
        if (userRefs > 0) {
            return Result.fail("该部门下仍有关联学员，无法删除");
        }
        boolean success = departmentService.removeById(id);
        return success ? Result.success(true) : Result.fail("删除失败");
    }
}
