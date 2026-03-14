package com.pxwork.api.controller.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pxwork.common.utils.Result;
import com.pxwork.course.entity.Course;
import com.pxwork.course.entity.CourseChapter;
import com.pxwork.course.service.CourseChapterService;
import com.pxwork.course.service.CourseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * 后台课程管理 前端控制器
 * </p>
 *
 * @author TraeAI
 * @since 2026-03-13
 */
@Tag(name = "后台课程管理", description = "后台课程相关的接口")
@RestController
@RequestMapping("/backend/course")
public class BackendCourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseChapterService courseChapterService;

    @Operation(summary = "课程分页列表", description = "获取所有课程，可根据名称或分类筛选")
    @GetMapping("/list")
    public Result<Page<Course>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String targetRole) {
        
        Page<Course> page = new Page<>(current, size);
        LambdaQueryWrapper<Course> queryWrapper = new LambdaQueryWrapper<>();
        
        if (categoryId != null && categoryId > 0) {
            queryWrapper.eq(Course::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(name)) {
            queryWrapper.like(Course::getName, name);
        }
        if (status != null) {
            queryWrapper.eq(Course::getStatus, status);
        }
        if (StringUtils.hasText(targetRole)) {
            queryWrapper.like(Course::getTargetRoles, targetRole);
        }
        queryWrapper.orderByDesc(Course::getCreatedAt);
        
        return Result.success(courseService.page(page, queryWrapper));
    }

    @Operation(summary = "创建课程")
    @PostMapping("/add")
    public Result<Boolean> create(@RequestBody Course course) {
        return Result.success(courseService.save(course));
    }

    @Operation(summary = "更新课程")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody Course course) {
        return Result.success(courseService.updateById(course));
    }

    @Operation(summary = "删除课程", description = "级联删除章节和课时")
    @PostMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        long chapterCount = courseChapterService.count(new LambdaQueryWrapper<CourseChapter>().eq(CourseChapter::getCourseId, id));
        if (chapterCount > 0) {
            // 这里也可以选择直接级联删除，取决于业务需求。根据之前CourseController的逻辑，这里选择提示或者直接调用级联删除。
            // 之前的逻辑是提示，但 Service 中有 removeCourseWithRelations 方法。
            // 为了方便后台操作，这里直接使用级联删除。
             return Result.success(courseService.removeCourseWithRelations(id));
        }
        return Result.success(courseService.removeById(id));
    }

    @Operation(summary = "获取课程详情")
    @GetMapping("/detail/{id}")
    public Result<Course> detail(@PathVariable Long id) {
        Course course = courseService.getCourseDetails(id);
        if (course == null) {
            return Result.fail("课程不存在");
        }
        return Result.success(course);
    }
}
