package com.pxwork.course.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pxwork.course.entity.CourseResource;

import java.util.List;
import java.util.Map;

public interface CourseResourceService extends IService<CourseResource> {
    Map<String, Object> bindResources(Long courseId, List<Long> resourceIds);

    List<Long> listResourceIdsByCourse(Long courseId);

    boolean unbindResource(Long courseId, Long resourceId);
}
