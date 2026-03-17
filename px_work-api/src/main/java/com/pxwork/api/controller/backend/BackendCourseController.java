package com.pxwork.api.controller.backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pxwork.common.utils.Result;
import com.pxwork.course.entity.Course;
import com.pxwork.course.entity.CourseChapter;
import com.pxwork.course.service.CourseResourceService;
import com.pxwork.course.service.CourseChapterService;
import com.pxwork.course.service.CourseService;
import com.pxwork.resource.entity.Resource;
import com.pxwork.resource.service.ResourceService;

import cn.dev33.satoken.annotation.SaCheckPermission;
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
@Tag(name = "2.1 后台-课程建设管理")
@RestController
@RequestMapping({"/backend/course", "/backend/courses"})
public class BackendCourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseChapterService courseChapterService;

    @Autowired
    private CourseResourceService courseResourceService;

    @Autowired
    private ResourceService resourceService;

    @Operation(summary = "课程分页列表", description = "获取所有课程，可根据名称或分类筛选")
    @SaCheckPermission("course:list")
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
    @SaCheckPermission("course:add")
    @PostMapping("/add")
    public Result<Boolean> create(@RequestBody Course course) {
        return Result.success(courseService.save(course));
    }

    @Operation(summary = "更新课程")
    @SaCheckPermission("course:update")
    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody Course course) {
        return Result.success(courseService.updateById(course));
    }

    @Operation(summary = "删除课程", description = "级联删除章节和课时")
    @SaCheckPermission("course:delete")
    @DeleteMapping("/delete/{id}")
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
    @SaCheckPermission("course:query")
    @GetMapping("/detail/{id}")
    public Result<Course> detail(@PathVariable Long id) {
        Course course = courseService.getCourseDetails(id);
        if (course == null) {
            return Result.fail("课程不存在");
        }
        return Result.success(course);
    }

    @Operation(summary = "绑定课程资料")
    @PostMapping("/{id}/bind-resources")
    public Result<Map<String, Object>> bindResources(@PathVariable Long id, @RequestBody List<Long> resourceIds) {
        try {
            return Result.success(courseResourceService.bindResources(id, resourceIds));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "获取课程资料列表")
    @GetMapping("/{id}/resources")
    public Result<List<Resource>> resources(@PathVariable Long id) {
        if (courseService.getById(id) == null) {
            return Result.fail("课程不存在");
        }
        List<Long> resourceIds = courseResourceService.listResourceIdsByCourse(id);
        if (resourceIds.isEmpty()) {
            return Result.success(List.of());
        }
        List<Resource> resources = resourceService.list(new LambdaQueryWrapper<Resource>().in(Resource::getId, resourceIds));
        Map<Long, Resource> resourceMap = new HashMap<>();
        for (Resource resource : resources) {
            resourceMap.put(resource.getId(), resource);
        }
        List<Resource> ordered = new ArrayList<>();
        Set<Long> seenIds = new java.util.HashSet<>();
        for (Long resourceId : resourceIds) {
            if (!seenIds.add(resourceId)) {
                continue;
            }
            Resource resource = resourceMap.get(resourceId);
            if (resource != null) {
                ordered.add(resource);
            }
        }
        return Result.success(ordered);
    }

    @Operation(summary = "解绑课程资料")
    @DeleteMapping("/{id}/resources/{resourceId}")
    public Result<Boolean> unbindResource(@PathVariable Long id, @PathVariable Long resourceId) {
        if (courseService.getById(id) == null) {
            return Result.fail("课程不存在");
        }
        boolean removed = courseResourceService.unbindResource(id, resourceId);
        if (!removed) {
            return Result.fail("课程未绑定该资料");
        }
        return Result.success(true);
    }
}
