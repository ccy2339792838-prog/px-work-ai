package com.pxwork.api.controller.course;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pxwork.common.utils.Result;
import com.pxwork.course.entity.CourseCategory;
import com.pxwork.course.service.CourseCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "课程分类管理")
@RestController
@RequestMapping("/course/category")
public class CourseCategoryController {

    @Autowired
    private CourseCategoryService courseCategoryService;

    @Operation(summary = "获取分类树形结构")
    @GetMapping("/tree")
    public Result<List<CourseCategory>> tree() {
        return Result.success(courseCategoryService.listTree());
    }

    @Operation(summary = "创建分类")
    @PostMapping
    public Result<Boolean> create(@RequestBody CourseCategory category) {
        return Result.success(courseCategoryService.save(category));
    }

    @Operation(summary = "更新分类")
    @PutMapping
    public Result<Boolean> update(@RequestBody CourseCategory category) {
        return Result.success(courseCategoryService.updateById(category));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        // 检查是否有子分类
        long count = courseCategoryService.count(new LambdaQueryWrapper<CourseCategory>().eq(CourseCategory::getParentId, id));
        if (count > 0) {
            return Result.fail("请先删除子分类");
        }
        return Result.success(courseCategoryService.removeById(id));
    }
}
