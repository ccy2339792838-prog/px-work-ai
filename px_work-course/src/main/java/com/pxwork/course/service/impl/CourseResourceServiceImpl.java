package com.pxwork.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pxwork.course.entity.Course;
import com.pxwork.course.entity.CourseResource;
import com.pxwork.course.mapper.CourseResourceMapper;
import com.pxwork.course.service.CourseResourceService;
import com.pxwork.course.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseResourceServiceImpl extends ServiceImpl<CourseResourceMapper, CourseResource> implements CourseResourceService {

    @Autowired
    private CourseService courseService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> bindResources(Long courseId, List<Long> resourceIds) {
        Course course = courseService.getById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        if (resourceIds == null || resourceIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要绑定的资料");
        }
        List<Long> distinctIds = resourceIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
        if (distinctIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要绑定的资料");
        }
        List<CourseResource> existingRelations = this.list(new LambdaQueryWrapper<CourseResource>()
                .eq(CourseResource::getCourseId, courseId)
                .in(CourseResource::getResourceId, distinctIds));
        Set<Long> existingIds = existingRelations.stream()
                .map(CourseResource::getResourceId)
                .collect(Collectors.toSet());
        List<Long> toAddIds = distinctIds.stream()
                .filter(id -> !existingIds.contains(id))
                .collect(Collectors.toList());
        List<CourseResource> toSave = new ArrayList<>();
        for (Long resourceId : toAddIds) {
            CourseResource relation = new CourseResource();
            relation.setCourseId(courseId);
            relation.setResourceId(resourceId);
            toSave.add(relation);
        }
        if (!toSave.isEmpty()) {
            this.saveBatch(toSave);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("courseId", courseId);
        result.put("requestedCount", distinctIds.size());
        result.put("alreadyBoundCount", existingIds.size());
        result.put("addedCount", toSave.size());
        result.put("addedResourceIds", toAddIds);
        return result;
    }

    @Override
    public List<Long> listResourceIdsByCourse(Long courseId) {
        return this.list(new LambdaQueryWrapper<CourseResource>()
                        .eq(CourseResource::getCourseId, courseId)
                        .orderByDesc(CourseResource::getCreatedAt))
                .stream()
                .map(CourseResource::getResourceId)
                .collect(Collectors.toList());
    }

    @Override
    public boolean unbindResource(Long courseId, Long resourceId) {
        return this.remove(new LambdaQueryWrapper<CourseResource>()
                .eq(CourseResource::getCourseId, courseId)
                .eq(CourseResource::getResourceId, resourceId));
    }
}
