package com.pxwork.api.controller.course;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pxwork.common.utils.Result;
import com.pxwork.course.entity.Course;
import com.pxwork.course.entity.CourseCategory;
import com.pxwork.course.service.CourseCategoryService;
import com.pxwork.course.service.CourseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "课程分类管理")
@RestController
@RequestMapping("/course/category")
public class CourseCategoryController {

    @Autowired
    private CourseCategoryService courseCategoryService;
    @Autowired
    private CourseService courseService;

    @Operation(summary = "获取分类树形结构")
    @GetMapping("/tree")
    public Result<List<CourseCategory>> tree(@RequestParam(required = false) String industry) {
        return Result.success(courseCategoryService.listTree(industry));
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
        long courseCount = courseService.count(new LambdaQueryWrapper<Course>().eq(Course::getCategoryId, id));
        if (courseCount > 0) {
            return Result.fail("该分类下仍有课程，无法删除");
        }
        return Result.success(courseCategoryService.removeById(id));
    }
}
