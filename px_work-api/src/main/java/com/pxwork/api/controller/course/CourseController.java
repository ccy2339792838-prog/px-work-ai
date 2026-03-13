package com.pxwork.api.controller.course;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pxwork.common.utils.Result;
import com.pxwork.course.entity.Course;
import com.pxwork.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "课程管理")
@RestController
@RequestMapping("/course")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Operation(summary = "课程分页列表")
    @GetMapping("/list")
    public Result<Page<Course>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name) {
        
        Page<Course> page = new Page<>(current, size);
        LambdaQueryWrapper<Course> queryWrapper = new LambdaQueryWrapper<>();
        
        if (categoryId != null && categoryId > 0) {
            queryWrapper.eq(Course::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(name)) {
            queryWrapper.like(Course::getName, name);
        }
        queryWrapper.orderByDesc(Course::getCreatedAt);
        
        return Result.success(courseService.page(page, queryWrapper));
    }

    @Operation(summary = "创建课程")
    @PostMapping
    public Result<Boolean> create(@RequestBody Course course) {
        return Result.success(courseService.save(course));
    }

    @Operation(summary = "更新课程")
    @PutMapping
    public Result<Boolean> update(@RequestBody Course course) {
        return Result.success(courseService.updateById(course));
    }

    @Operation(summary = "删除课程(级联删除章节和课时)")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(courseService.removeCourseWithRelations(id));
    }

    @Operation(summary = "获取课程详情(包含章节和课时)")
    @GetMapping("/{id}/details")
    public Result<Course> details(@PathVariable Long id) {
        Course course = courseService.getCourseDetails(id);
        if (course == null) {
            return Result.fail("课程不存在");
        }
        return Result.success(course);
    }
}
